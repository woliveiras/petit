## Description

Explain clearly what changed and why.

## Change Type

- [ ] feat
- [ ] fix
- [ ] docs
- [ ] refactor
- [ ] test
- [ ] chore

## Validation Steps

Test/reproduction steps:

1.
2.
3.

## Quality Checklist

- [ ] I ran `./gradlew assembleDebug && ./gradlew installDebug`
- [ ] I ran `./gradlew test`
- [ ] I ran `./gradlew spotlessCheck`
- [ ] I did not introduce secrets/credentials
- [ ] I updated required documentation
- [ ] I included screenshots/videos for visual changes (when applicable)

## Dependency PR Checklist (when applicable)

- [ ] PR is grouped by risk/type (build tooling, UI/charting, AndroidX runtime, or test/utils)
- [ ] Required dependency gates passed (`assembleDebug && installDebug`, `test`, `spotlessCheck`)
- [ ] Any update-caused regression is fixed in this PR or the risky dependency was removed from batch
- [ ] I reviewed [Dependency PR Merge Checklist](../docs/dependency-pr-merge-checklist.md)

## Security/Compliance Checklist

- [ ] Change is aligned with [SECURITY.md](../SECURITY.md)
- [ ] Change is aligned with [TRADEMARK_POLICY.md](../TRADEMARK_POLICY.md)
- [ ] No sensitive files were accidentally committed
- [ ] All commits in this PR are DCO signed (`Signed-off-by`)

### How to add DCO sign-off (quick help)

- New commit: `git commit -s -m "type(scope): message"`
- Configure once (recommended): `git config --global format.signoff true`
- Last commit already created without sign-off: `git commit --amend -s --no-edit`
- Multiple local commits without sign-off (before push): `git rebase -i HEAD~N` and run `git commit --amend -s --no-edit` for each one

## Related Links

- Issue(s):
- Document(s):
