'use strict';

let stompClient = null;
let username = null;
let roomId = null;
let userVote = null;
let hasAnnouncedVote = false;


const usernamePage = document.querySelector('#username-page');
const chatPage = document.querySelector('#chat-page');
const usernameForm = document.querySelector('#usernameForm');
const messageForm = document.querySelector('#messageForm');
const messageInput = document.querySelector('#message');
const messageArea = document.querySelector('#messageArea');
const connectingElement = document.querySelector('.connecting');

const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function extractRoomId() {
    const parts = window.location.pathname.split('/');
    roomId = parts.includes('game') ? parts[parts.length - 1] : 'default';
}

function connect(event) {
    username = document.querySelector('#name').value.trim();
    if (!username) return;

    extractRoomId();

    usernamePage.classList.add('hidden');
    chatPage.classList.remove('hidden');

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, onConnected, onError);
    event.preventDefault();
}

function onConnected() {
    stompClient.subscribe('/topic/' + roomId, onMessageReceived);

    stompClient.send("/app/chat.addUser", {}, JSON.stringify({
        sender: username,
        type: 'JOIN',
        roomId: roomId
    }));
    connectingElement.classList.add('hidden');
}

function onError(error) {
    connectingElement.textContent = 'Cannot join with server. Refresh page.';
    connectingElement.style.color = 'red';
}

window.addEventListener('beforeunload', () => {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
            sender: username,
            type: 'LEAVE',
            roomId: roomId
        }));
        stompClient.disconnect(); // close WebSocket connection
    }
});

function sendMessage(event) {
    const messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        const chatMessage = {
            sender: username,
            content: messageContent,
            type: 'CHAT',
            roomId: roomId
        };
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    console.log("ODEBRANO WIADOMOŚĆ:", message);

    const messageElement = document.createElement('li');

    // Type: USERS → update users list
    if (message.type === 'USERS') {
        updateUserList(message.content);
        return;
    }

    // Type: UPDATE_VOTE → only backend (without displaying)
    if (message.type === 'UPDATE_VOTE') {
        return;
    }

    // System types: JOIN / LEAVE / CLEAR
    if (['JOIN', 'LEAVE', 'CLEAR'].includes(message.type)) {
        messageElement.classList.add('event-message');

        let contentText = '';

        if (message.type === 'JOIN') {
            contentText = `${message.sender} joined!`;
        } else if (message.type === 'LEAVE') {
            contentText = `${message.sender} left the meeting!`;
        } else if (message.type === 'CLEAR') {
            if (message.sender === 'SYSTEM') return;
            contentText = `${message.sender} cleared the voting results.`;

            // Reset local state
            userVote = null;
            hasAnnouncedVote = false;
            document.querySelectorAll('.vote-btn').forEach(b => b.classList.remove('selected'));

            const votesOutput = document.getElementById('votesOutput');
            if (votesOutput) {
                votesOutput.textContent = '';
            }
        }

        const textElement = document.createElement('p');
        textElement.textContent = contentText;
        messageElement.appendChild(textElement);
        messageArea.appendChild(messageElement);
        messageArea.scrollTop = messageArea.scrollHeight;
        return;
    }

    // Type: VOTES → reformat block with results
    if (message.type === 'VOTES') {
        messageElement.classList.add('votes-message');

        const votesBlock = document.createElement('pre');
        votesBlock.style.whiteSpace = 'pre-wrap';
        votesBlock.style.margin = '0';
        votesBlock.textContent = message.content;

        messageElement.appendChild(votesBlock);
        messageArea.appendChild(messageElement);
        messageArea.scrollTop = messageArea.scrollHeight;
        return;
    }

    // Type default: CHAT – normal message (fe. vote)
    messageElement.classList.add('chat-message');

    const avatarElement = document.createElement('i');
    avatarElement.textContent = message.sender[0];
    avatarElement.style.backgroundColor = getAvatarColor(message.sender);
    messageElement.appendChild(avatarElement);

    const usernameElement = document.createElement('span');
    usernameElement.textContent = message.sender;
    messageElement.appendChild(usernameElement);

    const textElement = document.createElement('p');
    textElement.textContent = message.type === 'CHAT'
        ? `[${message.sender}] voted!`
        : message.content;

    messageElement.appendChild(textElement);
    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}


function updateUserList(jsonContent) {
    const users = JSON.parse(jsonContent);
    const userList = document.getElementById('userList');
    userList.innerHTML = '';

    for (const user in users) {
        const li = document.createElement('li');
        li.textContent = `${user} ${users[user] ? '✅' : '❌'}`;
        userList.appendChild(li);
    }
}

function getAvatarColor(name) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = 31 * hash + name.charCodeAt(i);
    }
    return colors[Math.abs(hash % colors.length)];
}

document.getElementById('generateGameBtn').addEventListener('click', () => {
    // Wyślij zapytanie do backendu, aby stworzyć nowy pokój
    fetch('/api/generateGame')
        .then(res => res.json())
        .then(data => {
            // Przekierowanie użytkownika do nowo utworzonego pokoju
            const sessionId = data.sessionId;  // Zakłada, że backend zwraca sessionId
            window.location.href = `/game/${sessionId}`;
        })
        .catch(err => {
            console.error("Błąd przy generowaniu pokoju:", err);
        });
});

document.querySelectorAll('.vote-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const vote = btn.getAttribute('data-value');

        // Podświetlenie wyboru
        document.querySelectorAll('.vote-btn').forEach(b => b.classList.remove('selected'));
        btn.classList.add('selected');

        userVote = vote;

        if (stompClient && username) {
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                sender: username,
                content: vote,
                type: 'UPDATE_VOTE', // Typ dla zmiany, nie ogłaszaj na czacie
                roomId: roomId
            }));

            if (!hasAnnouncedVote) {
                hasAnnouncedVote = true;

                stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                    sender: username,
                    content: vote,
                    type: 'CHAT', // Typ ogłoszenia na czacie
                    roomId: roomId
                }));
            }
        }
    });
});

document.getElementById('showVotesBtn').addEventListener('click', () => {
    fetch(`/api/votes/${roomId}`)
        .then(res => res.json())
        .then(data => {
            const votes = Object.entries(data);
            const output = votes.map(([user, vote]) => `${user}: ${vote}`).join('\n');

            const numericVotes = votes
                .map(([_, vote]) => parseFloat(vote))
                .filter(v => !isNaN(v));

            const avg = numericVotes.length > 0
                ? (numericVotes.reduce((a, b) => a + b, 0) / numericVotes.length).toFixed(2)
                : 'There are no votes...';

            const finalOutput = `📊 *Voting results:*\n${output}\n\n➡️ Average: ${avg}`;

            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                type: 'VOTES',
                content: finalOutput,
                roomId: roomId
            }));
        });
});

document.getElementById('clearVotesBtn').addEventListener('click', () => {
    fetch(`/api/votes/${roomId}`, { method: 'DELETE' })
        .then(() => {
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
                type: 'CLEAR',
                sender: username,
                roomId: roomId
            }));
        });
});

usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
