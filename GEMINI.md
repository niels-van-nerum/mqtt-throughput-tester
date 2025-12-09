# Merge Request (MR) Best Practices

This document outlines the best practices for creating and managing merge requests in this project. Following these guidelines will help us maintain a clean and understandable git history, and make the review process more efficient.

## 1. Before Creating an MR

*   **Create a new branch:** Always create a new branch for your changes from the `main` or `development` branch. Give your branch a descriptive name, like `feature/add-user-authentication` or `fix/login-page-bug`.
*   **Write clean code:** Ensure your code is well-formatted, documented, and follows the project's coding style.
*   **Test your changes:** Run all relevant tests to ensure that your changes don't break existing functionality and that your new functionality works as expected. Add new tests for new features.

## 2. Creating the MR

*   **Write a clear and descriptive title:** The title should be a short, one-line summary of the changes.
*   **Write a comprehensive description:**
    *   Explain the "what" and "why" of your changes.
    *   Link to any relevant issues or tickets.
    *   Include screenshots or GIFs for UI changes.
*   **Keep MRs small and focused:** Each MR should address a single concern. If you need to make multiple unrelated changes, create separate MRs.
*   **Add reviewers:** Add at least one reviewer to your MR. If you're not sure who to add, ask in the team's communication channel.

## 3. During the Review Process

*   **Respond to feedback:** Address any comments or questions from the reviewers.
*   **Push new commits:** If you need to make changes, push new commits to your branch. Avoid force-pushing unless absolutely necessary.
*   **Resolve conversations:** Once a comment or suggestion has been addressed, resolve the conversation to keep the MR clean.

## 4. After the MR is Approved

*   **Squash and merge:** Once the MR is approved, squash your commits into a single, meaningful commit and merge it into the target branch. Make sure the commit message follows the project's conventions.
*   **Delete the branch:** After the MR is merged, delete your feature branch.
