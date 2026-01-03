# Contributing

This guide keeps expectations clear and simple so you can move fast without guessing.

## Before Starting

- Check open issues to avoid duplicating work.

## Standard Workflow

1. Fork repository.
2. Clone fork: `git clone https://github.com/<your-user>/jagfx-scala.git`.
3. Create topic branch: `git checkout -b feat/<short-description>`.
4. Make focused changes.
5. Run `sbt test` (and any extra commands relevant to change).
6. Commit with clear message explaining *why* change exists.
7. Push and open pull request describing behaviour changes and tests.

## Coding Guidelines

### Core Principles

- **KISS** -- prefer simplest solution that works today.
- **DRY** -- extract shared behaviour quickly to keep one source of truth.
- **YAGNI** -- DO NOT build future features until roadmap calls for them.

### Comments and Docs

- Comment to explain *why* choice was made, not what code already states.
- Update documentation and examples whenever behaviour changes.

## Testing Expectations

- Run `sbt test` before every push.
- Add or update targeted tests for every bug fix or new feature.
- Mention any skipped or flaky tests in pull request so reviewers know risk.

## Using AI Assistants

No AI configuration files live in repository yet, but you may use own tools. You remain responsible for code quality:

- Understand surrounding code before pasting AI suggestions.
- Review and test generated code carefully.
- Never merge output you do not fully understand.

## Pull Request Checklist

- [ ] Tests pass locally with `sbt test`.
- [ ] Docs and comments updated if behaviour changed.
- [ ] Commit messages explain intent.
- [ ] PR description covers motivation, approach, and testing.

## Reporting Issues

When filing bug, include:

- Steps to reproduce.
- Expected versus actual behaviour.
- Output snippets or logs when helpful.
- Commit hash, tooling version, and platform.

Feature requests should describe use case and why this needs it now.

## Questions and Support

Open issue if you need clarification on direction, architecture, or roadmap priorities. Discussions stay public so future contributors benefit from context.

## Code of Conduct

All contributors must follow [Code of Conduct](CODE_OF_CONDUCT.md).
