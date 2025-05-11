'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
var username = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
    username = document.querySelector('#name').value.trim();

    if(username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        var socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}


function onConnected() {
    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.addUser",
        {},
        JSON.stringify({sender: username, type: 'JOIN'})
    )

    connectingElement.classList.add('hidden');
}


function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}


function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    if(messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageInput.value,
            type: 'CHAT',
            roomId: "0xx"
        };
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}


function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);

    var messageElement = document.createElement('li');

    if (message.type === 'USERS') {
        updateUserList(message.content);  // zawiera JSON string z mapą
        return;
    } else if (message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined!';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!';
    } else if (message.type === 'CLEAR') {
        messageElement.classList.add('event-message');
        message.content = "Wszystkie głosy zostały wyczyszczone.";
    } else if (message.type === 'VOTES') {
        messageElement.classList.add('votes-message');
        message.content = "Głosy wszystkich użytkowników:\n" + message.content;
    } else {
        // Zwykła wiadomość użytkownika
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        var avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        var usernameElement = document.createElement('span');
        var usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    // Treść wiadomości z prefiksem
    var textElement = document.createElement('p');
    var prefixedContent = message.content;

    // Dodaj prefix tylko do wiadomości CHAT
    if (message.type === 'CHAT') {
        prefixedContent = `[${message.sender}] już zagłosował!`; // Prefix do wiadomości CHAT
    }

    var messageText = document.createTextNode(prefixedContent);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    // Dodaj wiadomość do obszaru chatu
    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function updateUserList(jsonContent) {
    const userMap = JSON.parse(jsonContent);
    const userList = document.getElementById('userList');
    userList.innerHTML = '';

    for (let user in userMap) {
        const li = document.createElement('li');
        li.textContent = `${user} ${userMap[user] ? '✅' : '❌'}`;
        userList.appendChild(li);
    }
}

function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }
    var index = Math.abs(hash % colors.length);
    return colors[index];
}

document.querySelector('#showVotesBtn').addEventListener('click', function() {
    // Wysyłanie zapytania do API o głosy
    fetch('/api/votes')
        .then(response => response.json())
        .then(data => {
            let output = '';
            for (let user in data) {
                output += `${user}: ${data[user]}\n`;
            }

            document.querySelector('#votesOutput').textContent = output || "Brak głosów.";

            // Wysyłanie głosów przez WebSocket na temat VOTES
            var votesMessage = {
                type: 'VOTES',
                content: output
            };
            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(votesMessage));
        })
        .catch(err => {
            alert('Błąd podczas pobierania danych z serwera');
            console.error(err);
        });
});

document.addEventListener("DOMContentLoaded", function () {
    const votesBtn = document.getElementById('showVotesBtn');
    const clearBtn = document.getElementById('clearVotesBtn');
    const votesOutput = document.getElementById('votesOutput');
    const messageArea = document.getElementById('messageArea');

    if (votesBtn) {
        votesBtn.addEventListener('click', function () {
            fetch('/api/votes')
                .then(response => response.json())
                .then(data => {
                    let output = '';
                    for (let user in data) {
                        output += `${user}: ${data[user]}\n`;
                    }
                    votesOutput.textContent = output || "Brak głosów.";
                })
                .catch(err => {
                    alert('Błąd podczas pobierania głosów z serwera');
                    console.error(err);
                });
        });
    }

    if (clearBtn) {
        clearBtn.addEventListener('click', function () {
            fetch('/api/votes', { method: 'DELETE' })
                .then(() => {
                    votesOutput.textContent = '';
                    messageArea.innerHTML = ''; // czyści wszystkie wiadomości z chatu
                    alert('Głosy i czat zostały wyczyszczone.');
                })
                .catch(err => {
                    alert('Błąd podczas czyszczenia danych');
                    console.error(err);
                });
        });
    }
});

// JavaScript do obsługi przycisku "Generate Game"
document.getElementById('generateGameBtn').addEventListener('click', function () {
    // Generowanie unikalnego identyfikatora sesji (np. UUID lub losowy ciąg)
    const sessionId = generateUniqueSessionId();

    // Przekierowanie użytkownika do nowego URL z identyfikatorem sesji
    window.location.href = `/game/${sessionId}`;
});

// Funkcja do generowania unikalnego identyfikatora sesji
function generateUniqueSessionId() {
    return 'game-' + Math.random().toString(36).substr(2, 9);  // Generuje losowy identyfikator
}

document.querySelectorAll('.vote-btn').forEach(btn => {
    btn.addEventListener('click', function () {
        const selectedValue = this.getAttribute('data-value');

        if (selectedValue && stompClient) {
            const chatMessage = {
                sender: username,
                content: selectedValue,
                type: 'CHAT',
                roomId: "0xx"
            };

            stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        }
    });
});




usernameForm.addEventListener('submit', connect, true)
messageForm.addEventListener('submit', sendMessage, true)