"""Safe text extraction for user-provided documents."""

from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO
from pathlib import PurePath
import zipfile

from docx import Document
from pypdf import PdfReader

from app.services.security_service import security_scanner


MAX_UPLOAD_BYTES = 8 * 1024 * 1024
MAX_EXTRACTED_CHARS = 50_000
MAX_PDF_PAGES = 100
MAX_DOCX_MEMBERS = 200
MAX_DOCX_UNCOMPRESSED_BYTES = 20 * 1024 * 1024
SUPPORTED_EXTENSIONS = {".txt", ".md", ".pdf", ".docx"}


class DocumentExtractionError(ValueError):
    """Raised when a document is unsupported, malformed, or exceeds safe limits."""


@dataclass
class ExtractedDocument:
    filename: str
    file_type: str
    text: str
    character_count: int
    page_count: int | None
    security_findings: list[dict[str, str]]
    contains_untrusted_instructions: bool


def _extract_text(data: bytes, extension: str) -> tuple[str, int | None]:
    if extension in {".txt", ".md"}:
        if b"\x00" in data:
            raise DocumentExtractionError("Binary content is not valid plain text.")
        try:
            return data.decode("utf-8-sig"), None
        except UnicodeDecodeError as exc:
            raise DocumentExtractionError("Text files must use UTF-8 encoding.") from exc

    if extension == ".pdf":
        if not data.startswith(b"%PDF-"):
            raise DocumentExtractionError("The uploaded file is not a valid PDF.")
        try:
            reader = PdfReader(BytesIO(data), strict=True)
            if reader.is_encrypted:
                raise DocumentExtractionError("Password-protected PDFs are not supported.")
            if len(reader.pages) > MAX_PDF_PAGES:
                raise DocumentExtractionError(f"PDF exceeds the {MAX_PDF_PAGES}-page limit.")
            pages = [(page.extract_text() or "") for page in reader.pages]
            return "\n\n".join(pages), len(pages)
        except DocumentExtractionError:
            raise
        except Exception as exc:
            raise DocumentExtractionError("The PDF could not be parsed.") from exc

    if extension == ".docx":
        try:
            with zipfile.ZipFile(BytesIO(data)) as archive:
                members = archive.infolist()
                if len(members) > MAX_DOCX_MEMBERS:
                    raise DocumentExtractionError("DOCX contains too many embedded files.")
                expanded_size = sum(member.file_size for member in members)
                if expanded_size > MAX_DOCX_UNCOMPRESSED_BYTES:
                    raise DocumentExtractionError("DOCX expands beyond the safe size limit.")
                if "word/document.xml" not in archive.namelist():
                    raise DocumentExtractionError("The uploaded DOCX is missing its document body.")
            document = Document(BytesIO(data))
            parts = [paragraph.text for paragraph in document.paragraphs if paragraph.text.strip()]
            for table in document.tables:
                for row in table.rows:
                    cells = [cell.text.strip() for cell in row.cells if cell.text.strip()]
                    if cells:
                        parts.append(" | ".join(cells))
            return "\n".join(parts), None
        except DocumentExtractionError:
            raise
        except Exception as exc:
            raise DocumentExtractionError("The DOCX could not be parsed.") from exc

    raise DocumentExtractionError("Unsupported file type. Upload TXT, MD, PDF, or DOCX.")


def extract_document(filename: str, data: bytes) -> ExtractedDocument:
    """Extracts text in memory, bounds resource use, and flags suspicious content."""
    safe_name = PurePath((filename or "").replace("\\", "/")).name
    extension = PurePath(safe_name).suffix.lower()
    if not safe_name or extension not in SUPPORTED_EXTENSIONS:
        raise DocumentExtractionError("Unsupported file type. Upload TXT, MD, PDF, or DOCX.")
    if not data:
        raise DocumentExtractionError("The uploaded file is empty.")
    if len(data) > MAX_UPLOAD_BYTES:
        raise DocumentExtractionError("File exceeds the 8 MB upload limit.")

    text, page_count = _extract_text(data, extension)
    text = text.replace("\x00", "").strip()
    if not text:
        raise DocumentExtractionError("No readable text was found in the document.")
    if len(text) > MAX_EXTRACTED_CHARS:
        text = text[:MAX_EXTRACTED_CHARS]

    scan = security_scanner.scan(text, redact=True)
    findings = [
        {
            "pattern": item.pattern,
            "threat_level": item.threat_level,
            "description": item.description,
        }
        for item in scan.findings
    ]
    return ExtractedDocument(
        filename=safe_name,
        file_type=extension.lstrip("."),
        text=scan.clean_text,
        character_count=len(scan.clean_text),
        page_count=page_count,
        security_findings=findings,
        contains_untrusted_instructions=any(
            item["pattern"] in {"prompt_override", "identity_override", "disregard_instructions"}
            for item in findings
        ),
    )
