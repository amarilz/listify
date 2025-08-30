### Listify
A powerful Kotlin-based tool that consolidates your entire project's source code into a single file for seamless AI analysis and collaboration.
##### Overview
Listify is a directory processing application that recursively scans your project folders and combines all source files into one unified text file. In today's AI-driven development landscape, this tool bridges the gap between your codebase and Large Language Models (LLMs), enabling comprehensive code analysis, debugging, and optimization.
##### Why Listify?
###### 🤖 Perfect for AI/LLM Integration
- **Instant Context Sharing**: Provide complete project context to ChatGPT, Claude, or any LLM in seconds
- **Comprehensive Code Analysis**: Enable AI to understand your entire codebase structure and relationships
- **Efficient Problem Solving**: Allow LLMs to identify cross-file dependencies, architectural issues, and optimization opportunities
- **Code Review Assistance**: Get holistic feedback on your entire project rather than fragmented file-by-file analysis
##### How It Works
1. **Select Directory**: Choose your project root folder
2. **Configure Filters**: Specify prefixes to exclude (optional)
3. **Process**: Listify recursively scans and consolidates all files
4. **Output**: Generates `listify.txt` in your selected directory
###### Output Format
```
=== File: /path/to/your/project/src/Main.kt ===
[file content here]


=== File: /path/to/your/project/src/Utils.kt ===
[file content here]
```
