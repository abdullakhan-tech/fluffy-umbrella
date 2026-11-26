# Zaythar AI GitHub Discussions

Use GitHub Discussions for community conversations that do not need a code change or a tracked bug-fix workflow. Use Issues for reproducible defects and actionable implementation tasks; use Pull Requests for proposed code/documentation changes.

## Recommended categories

Create these categories in **Repository → Settings → General → Features → Discussions → Set up discussions** (the category setup itself is a GitHub UI action):

| Category | Format | Purpose |
|---|---|---|
| Announcements | Announcement | Maintainer updates, releases, service notices, and project milestones. |
| Ideas & Feature Requests | Open-ended | Suggestions, use cases, and community voting. |
| Questions & Help | Q&A | Setup, troubleshooting, configuration, and usage questions. |
| Show and Tell | Open-ended | Demos, screenshots, workflows, integrations, and community projects. |
| Architecture & Roadmap | Open-ended | Design proposals, trade-offs, compatibility, and roadmap discussion. |

The YAML files in `.github/DISCUSSION_TEMPLATE/` provide structured prompts for new discussions. GitHub category creation and assignment remain repository settings actions; adding templates does not create categories automatically.

## Posting guidelines

1. Search existing discussions before opening a duplicate.
2. Choose the closest category and use a descriptive title.
3. Include the app/platform, version or commit when relevant, exact steps, expected result, and actual result.
4. Remove API keys, Firebase configuration, access tokens, personal data, private logs, and screenshots containing secrets before posting.
5. For security vulnerabilities, do not publish exploit details or credentials in a public discussion. Contact the maintainers privately using the repository's published security contact or GitHub's private vulnerability reporting if enabled.
6. Keep feedback respectful, specific, and focused on Zaythar AI.

## Where to report work

- **Bug or regression:** open a GitHub Issue with reproducible steps.
- **Feature implementation:** open an Issue first when scope is unclear, then submit a Pull Request.
- **Usage question:** use Questions & Help.
- **Security issue:** use a private reporting channel, not a public thread.
- **Release or project update:** maintainers post in Announcements.

## Maintainer workflow

- Welcome and tag new contributors; link duplicate or related discussions.
- Convert actionable, agreed work into Issues so it can be prioritized and tracked.
- Mark solved Q&A threads as answered.
- Summarize decisions in the relevant issue, pull request, or architecture documentation so important decisions are not trapped in a discussion thread.
- Close or lock threads only when resolved, obsolete, or clearly abusive.

## Templates

- `.github/DISCUSSION_TEMPLATE/announcements.yml`
- `.github/DISCUSSION_TEMPLATE/ideas-feature-requests.yml`
- `.github/DISCUSSION_TEMPLATE/questions-help.yml`
- `.github/DISCUSSION_TEMPLATE/show-and-tell.yml`
- `.github/DISCUSSION_TEMPLATE/architecture-roadmap.yml`

## GitHub setup checklist

- [ ] Enable Discussions in repository features.
- [ ] Create the five categories listed above and select the matching format.
- [ ] Add category descriptions and assign maintainers/moderators.
- [ ] Confirm the discussion templates appear when creating a new discussion.
- [ ] Publish a short welcome/house-rules announcement.
- [ ] Review security reporting settings and ensure a private route is available.
