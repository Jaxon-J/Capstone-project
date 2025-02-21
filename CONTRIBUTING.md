# Opening a branch

- Create an issue
  - The issue should state your intent on what you are trying to add, or what you are modifying in the repo.
  - Assignees: whoever is relevant
  - Projects: "Board Tracking"
  - Milestone optional
  - Once created, assign `Status` under Projects
- Create a new branch
  - From the command line:
    - `git switch -c [branch name]` - switches to the branch you want to develop on (-c creates the branch if it doesn't exist)
    - `git push -u origin [branch name]` - pushes branch to Github
  - From Android Studio:
    - Open Git menu on bottom left
    - Click `HEAD` from the branch listing, then the `+` to create branch.
    - Right click the created branch under `Local` and click `Push...`, then Ok.
    - Right click the new branch under `Local` and click `Checkout` to start working under this branch.
- Back to Github
  - Development: Assign the newly created branch to the issue.
  - Further issues being worked on under this branch can also be assigned to this branch.

- This branch can be re-utilized for multiple issues, it's just an alternative path for development. 

# Pull Requests

- If you want to update the main branch, get onto Github, navigate to your branch, click the `Contribute` button and `Open Pull Request`.
- If it's a big PR, add me (lith-x) as a `Reviewer`, I'll take a look at it and see if there are any issues.
- Development: please put any issues that are resolved by the PR, remove the ones that aren't. Any listed issues are auto-closed with notes that it was closed via PR.
