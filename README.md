Well, I found out that most existing tools for planning poker are either paid or limited by trial periods,
So I thought: why not just build one?

This is a lightweight web application for planning poker,
I wanted the voting process to feel like a real-time group chat, so I wired everything through WebSockets to simulate that shared-room vibe.

Built with:
- Java + Spring Boot for the backend
- WebSockets for real-time communication
- Vanilla Frontend for a clean,
- I went old-school—just JavaScript, HTML, and CSS. Sometimes less is more.

🔧 Key Features:
Hidden votes until all team members submit
Automatic average calculation of numeric votes
Option to clear votes and restart estimation