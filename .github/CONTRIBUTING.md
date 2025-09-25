# Contributing to Microbot

Thanks for your interest in contributing!  
Please read this guide carefully before opening a pull request.

---

## Branching Model

- **`development` branch**  
  All contributors must branch off from `development`.  
  Pull requests are only accepted into `development`.  

- **`main` branch**  
  Protected. Only **Mocrosoft** updates this branch by merging `development` → `main`.  

- **Repository scope**  
  This repository accepts **client changes only**.  
  **Scripts are not allowed** here and will not be merged.  

---

## Contribution Flow

```mermaid
gitGraph
   commit id: "main"
   branch development
   commit id: "dev work"
   branch feature/my-change
   commit id: "my feature"
   commit id: "improve feature"
   checkout development
   merge feature/my-change id: "PR -> development"
   checkout main
   merge development id: "Mocrosoft merges"
