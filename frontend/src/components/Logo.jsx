import React from 'react';

const Logo = () => {
    return (
        <div className="logo-container">
            <svg
                className="logo-icon"
                viewBox="0 0 100 100"
                xmlns="http://www.w3.org/2000/svg"
            >
                <defs>
                    <linearGradient id="eyeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#60a5fa" />
                        <stop offset="100%" stopColor="#a855f7" />
                    </linearGradient>
                    <linearGradient id="headGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stopColor="#1e293b" />
                        <stop offset="100%" stopColor="#0f172a" />
                    </linearGradient>
                </defs>

                {/* Yttre ring */}
                <circle cx="50" cy="50" r="46" fill="url(#headGradient)" stroke="#334155" strokeWidth="3" />
                <circle cx="50" cy="50" r="46" fill="none" stroke="#3b82f6" strokeWidth="1.5" strokeDasharray="10 15" className="spinning-ring" />

                {/* Ögon */}
                <rect x="30" y="35" width="10" height="18" rx="5" fill="url(#eyeGradient)" />
                <rect x="60" y="35" width="10" height="18" rx="5" fill="url(#eyeGradient)" />

                {/* Blink */}
                <circle cx="35" cy="40" r="2" fill="#ffffff" />
                <circle cx="65" cy="40" r="2" fill="#ffffff" />

                {/* Leende */}
                <path
                    d="M 32 62 Q 50 74 68 62"
                    fill="none"
                    stroke="url(#eyeGradient)"
                    strokeWidth="5"
                    strokeLinecap="round"
                />
            </svg>

            <span className="logo-text">
        Funny<span className="logo-text-gradient">AI</span>
      </span>
        </div>
    );
};

// Detta gör att vi kan importera komponenten i andra filer
export default Logo;