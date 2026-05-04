import React, { useState, useEffect, useRef } from 'react';
import './App.css';
import { v4 as uuidv4 } from 'uuid';
import { Send, User, Bot, Sparkles, MessageCircle, Zap, Ghost, Laugh } from 'lucide-react';

function App() {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [personality, setPersonality] = useState('helper');
    const [sessionId, setSessionId] = useState('');
    const [loading, setLoading] = useState(false);

    // Referens för auto-scroll
    const messagesEndRef = useRef(null);

    const personalities = [
        { id: 'helper', name: 'Pappa-humor', icon: <MessageCircle size={16} /> },
        { id: 'sarcastic', name: 'Sarkastisk', icon: <Zap size={16} /> },
        { id: 'chaos', name: 'Kaos-läge', icon: <Ghost size={16} /> }
    ];

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, loading]);

    useEffect(() => {
        let id = localStorage.getItem('chat_session_id');
        if (!id) {
            id = uuidv4();
            localStorage.setItem('chat_session_id', id);
        }
        setSessionId(id);
    }, []);

    const sendMessage = async (e) => {
        e.preventDefault();
        if (!input.trim() || loading) return;

        const userMessage = { role: 'user', content: input };
        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setLoading(true);

        try {
            const response = await fetch('http://localhost:8080/api/v1/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    message: input,
                    personality: personality,
                    sessionId: sessionId
                }),
            });

            const data = await response.json();
            const aiMessage = { role: 'assistant', content: data.reply };
            setMessages(prev => [...prev, aiMessage]);
        } catch (error) {
            console.error("Fel vid anrop:", error);
            setMessages(prev => [...prev, { role: 'assistant', content: "Hoppsan! Kunde inte nå servern." }]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="chat-container">
            <span className="session-id-discrete">ID: {sessionId.slice(0, 8)}</span>

            <header>
                <div className="header-top">
                    <h1>FunnyAI <Laugh size={18} className="sparkle-icon" /></h1>
                </div>

                <div className="personality-selector">
                    {personalities.map((p) => (
                        <button
                            key={p.id}
                            className={`personality-chip ${personality === p.id ? 'active' : ''}`}
                            onClick={() => setPersonality(p.id)}
                        >
                            {p.icon}
                            {p.name}
                        </button>
                    ))}
                </div>
            </header>

            <div className="message-list">
                {messages.length === 0 && (
                    <div className="empty-state">
                        <Bot size={48} />
                        <p>Välkommen! Välj en personlighet ovan och börja chatta på svenska.</p>
                    </div>
                )}
                {messages.map((msg, idx) => (
                    <div key={idx} className={`message ${msg.role}`}>
                        <div className="avatar">
                            {msg.role === 'user' ? <User size={18} /> : <Bot size={18} />}
                        </div>
                        <div className="bubble">
                            <p>{msg.content}</p>
                        </div>
                    </div>
                ))}
                {loading && (
                    <div className="message assistant">
                        <div className="avatar pulsing"><Bot size={18} /></div>
                        <div className="bubble loading-bubble">
                            <div className="dot"></div><div className="dot"></div><div className="dot"></div>
                        </div>
                    </div>
                )}
                {/* Ankare för auto-scroll */}
                <div ref={messagesEndRef} />
            </div>

            <div className="input-area">
                <form onSubmit={sendMessage}>
                    <input
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        placeholder={`Prata med din ${personalities.find(p => p.id === personality).name}...`}
                    />
                    <button type="submit" disabled={loading || !input.trim()}>
                        <Send size={20} />
                    </button>
                </form>
            </div>
        </div>
    );
}

export default App;