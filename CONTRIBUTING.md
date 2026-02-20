# Contributing

Welcome to Galasa's webui! To learn more about contributing to the Galasa project, please read this Contributor's Guide.

## How can you contribute?

### Reporting bugs

- Search existing issues to avoid duplicates.
- Include clear and concise steps on how to reproduce the bug.
- Provide relevant details, such as your Galasa version and details about environment.
- Raise bugs [here](https://github.com/galasa-dev/projectmanagement/issues).

### Suggesting features

- Open an issue and include a user story, background if relevant, and task list.
- Provide a clear description of the feature.
- Explain why it would be beneficial and how it aligns with the project's goals.
- Raise feature suggestions, as user stories, [here](https://github.com/galasa-dev/projectmanagement/issues).

### Contributing code

- Check out open issues on [our Kanban board](https://github.com/orgs/galasa-dev/projects/3), especially ones with the label `good first issue`.
- Other common labels include `webui`, `cli` or `REST API`. Typically, stories without one of these three tags will imply it's a change needing to be made to this repository.

### Abide by the contributions legal guidance
To ship, all developer contributions must abide by the legal guidance detailed in the [Developer Certificate of Origin](./CONTRIBUTIONS.md)

### Documentation

- Fix typos, improve examples, or enhance explanations.

## How to make a contribution to this Repository

### Sign your commits

Make sure you are able to sign commits with your personal GPG key. See https://git-scm.com/book/ms/v2/Git-Tools-Signing-Your-Work

Whenever you commit, please sign commits with `-s -S` flags to sign the commit.
This allows us to prove who made each change to the codebase.

Each PR build has "Developer Certificate of Origin" [DCO](./CONTRIBUTIONS.md) checking turned on, so nothing will get
delivered without signed commits.

If you forgot to sign one or all of your commits, you can squash your PR changes, sign them, then force-push your branch.

### Set up a fork of a repository

1. On GitHub, navigate to the repository.
1. In the top-right corner of the page, click Fork.
1. Select an owner for the forked repository from the dropdown menu under "Owner".
1. The fork will be named the same as the upstream repository as default. Optionally, to further distinguish your fork, type a name in the "Repository name" field.
1. Optionally, type a description for your fork in the "Description" field.
1. Optionally, select "Copy the `main` branch only".
1. Click "Create fork".

### Clone the forked repository and make changes
1. Clone your forked repository to your machine:
```
git clone https://github.com/YOUR-USERNAME/webui.git
```
2. Make your changes and ensure they build locally with the `/build-locally.sh` script and that the unit tests pass.

### Contribute code back to the project
1. Add the original repository, `upstream`, as a remote, and ensure you cannot push to it:
```
# replace <upstream git repo> with the upstream repo URL
# example:
#  https://github.com/galasa-dev/webui.git
#  git@github.com/galasa-dev/webui.git

git remote add upstream <upstream git repo>
git remote set-url --push upstream no_push
```
2. Verify this step by listing your configured remote repositories:
```
git remote -v
```
3. Create a new branch for your contribution:
```
git checkout -b issue-number/contribution-description
```
4. Make your changes and commit them, ensuring to DCO and GPG sign your commits:
Please use https://www.conventionalcommits.org/en/v1.0.0/ as a guide for making commits, in the. format `type(scope)!: description` (Scope and ! for breaking changes are optional), where types include: 
- feat: A new feature.
- fix: A bug fix.
- docs: Documentation changes.
- style: Formatting, missing semicolons, etc..
- refactor: Code change that neither fixes a bug nor adds a feature.
- test: Adding missing tests or correcting existing tests.
- build: Changes that affect the build system or external dependencies.
- ci: Changes to CI configuration files and scripts.

For example, `feat(auth): add JWT token refresh endpoint`,
```
git commit -s -S -m "Add a meaningful commit message"
```
5. Push your changes to your fork:
```
git push origin issue-number/contribution-description
```
6. Open a pull request from your forked repository branch to the main branch of the 'webui repository', and explain your changes. Refer to any stories which are relevent and explain why the change was made, what the change is, and anything else which reviewers would find helpful to understand the context of the change.
