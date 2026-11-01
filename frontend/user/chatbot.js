(() => {
    if (document.getElementById("smartchargeChatPanel")) return;

    const API_BASE_URL = ((location.protocol === "file:" || location.hostname === "localhost" || location.hostname === "127.0.0.1")
        ? "http://localhost:8080/api" : "/api");

    const knowledge = [
        {
            title: "Charging requests",
            topics: [
                {
                    label: "How do I create a charging request?",
                    answer: "Open the booking page, choose your vehicle, station, and charger, then enter your current and target battery levels. Review the details before submitting. You can follow the request from your status page.",
                    links: [{ label: "Open booking", href: "booking.html" }, { label: "View request status", href: "status.html" }]
                },
                {
                    label: "What do the request statuses mean?",
                    answer: "Pending means your request is waiting in the queue. Allocated means a charger has been assigned. Active means the simulated charging session is running. Completed means the session finished. Cancelled means the request was cancelled before charging began.",
                    links: [{ label: "View my requests", href: "status.html" }]
                },
                {
                    label: "Check my latest request",
                    action: "latestRequest"
                },
                {
                    label: "Can I cancel a request?",
                    answer: "A waiting request can be cancelled from its status page. Requests that are already allocated or charging cannot be cancelled through the driver flow. Open your requests to check the current status and available action.",
                    links: [{ label: "Open request status", href: "status.html" }]
                },
                {
                    label: "How does the queue and wait estimate work?",
                    answer: "Requests wait until a compatible charger is available. SmartCharge displays an estimate using current charger and queue information. The estimate can change when charger availability or the queue changes.",
                    links: [{ label: "Choose a station", href: "booking.html" }, { label: "Explore stations", href: "remote.html" }]
                },
                {
                    label: "What happens to an emergency-priority request?",
                    answer: "You can request priority while making a booking and provide a reason. In the current workflow, an administrator reviews the request and decides whether to approve or decline the priority. This does not replace real-world emergency services.",
                    links: [{ label: "Make a booking", href: "booking.html" }, { label: "Check my requests", href: "status.html" }]
                }
            ]
        },
        {
            title: "Vehicles and chargers",
            topics: [
                {
                    label: "How do I add a vehicle?",
                    answer: "Open Vehicle Management and add your vehicle details, including its battery capacity and connector type. You can then choose it when making a charging request.",
                    links: [{ label: "Manage vehicles", href: "vehicle.html" }, { label: "Book charging", href: "booking.html" }]
                },
                {
                    label: "How do I choose a compatible charger?",
                    answer: "Select a station and review the chargers available there. Choose a charger with a connector compatible with your vehicle. If no compatible charger is available, choose another station or try again later.",
                    links: [{ label: "Start a booking", href: "booking.html" }, { label: "Explore stations", href: "remote.html" }]
                },
                {
                    label: "Where can I update my vehicle details?",
                    answer: "Use Vehicle Management to review or update your saved vehicle information. Your vehicle details are associated with your account.",
                    links: [{ label: "Manage vehicles", href: "vehicle.html" }]
                }
            ]
        },
        {
            title: "Stations, maps and routes",
            topics: [
                {
                    label: "How do I find a station?",
                    answer: "Open the Remote view to explore stations and map information. You can also choose a station during booking. If location features are used, allow location access in your browser when prompted.",
                    links: [{ label: "Explore stations", href: "remote.html" }, { label: "Choose a station", href: "booking.html" }]
                },
                {
                    label: "Why is my location or route missing?",
                    answer: "Check that location access is enabled for this site and that your browser allows location services. If you decline the prompt, you can still browse stations manually. Route availability can also depend on the routing service.",
                    links: [{ label: "Open Remote view", href: "remote.html" }]
                },
                {
                    label: "How do station wait estimates work?",
                    answer: "Wait estimates use the station's charger availability, active charging sessions, and queued requests. They are estimates, so actual waits may change as requests and charger status change.",
                    links: [{ label: "Explore stations", href: "remote.html" }]
                }
            ]
        },
        {
            title: "Account and email verification",
            topics: [
                {
                    label: "How do I verify my email?",
                    answer: "During sign-up, SmartCharge sends a verification code to your email address. Enter that code on the verification step to finish creating your account. Check your spam or junk folder if it does not arrive.",
                    links: [{ label: "Go to sign in or sign up", href: "login.html" }]
                },
                {
                    label: "My verification email did not arrive",
                    answer: "Check that you entered the right email address, then check your spam or junk folder. The sign-up screen provides a resend option after its waiting period. If you still cannot receive the code, contact the project administrator.",
                    links: [{ label: "Open sign-in page", href: "login.html" }]
                },
                {
                    label: "I cannot log in",
                    answer: "Check that you are using the email address and password associated with your account. Email matching ignores capitalization, but the password is case-sensitive. If you are still unable to sign in, contact the project administrator.",
                    links: [{ label: "Open sign-in page", href: "login.html" }]
                }
            ]
        },
        {
            title: "Charging safety and SmartCharge",
            topics: [
                {
                    label: "Is SmartCharge connected to a real charger?",
                    answer: "No. This project simulates charging requests, charger allocation, and charging-session progress. It does not control physical charging equipment or process real payments. Do not rely on the app for real-world charger safety or emergency response."
                },
                {
                    label: "I see smoke, sparks, overheating or another immediate hazard",
                    answer: "Stop using the equipment if you can do so safely, move away, and do not touch or attempt to repair it. Follow the site's emergency procedures and contact local emergency services or qualified station staff. SmartCharge cannot detect or shut down physical equipment.",
                    urgent: true
                },
                {
                    label: "How do I ask for priority because of an urgent situation?",
                    answer: "When making a booking, use the emergency-priority option and describe the reason clearly. The current SmartCharge workflow sends the priority request to an administrator for review. For immediate danger, use local emergency procedures rather than relying on this app.",
                    links: [{ label: "Open booking", href: "booking.html" }]
                }
            ]
        },
        {
            title: "Payments and charging sessions",
            topics: [
                {
                    label: "Does SmartCharge take real payments?",
                    answer: "No. Payment History and payment attempts are simulations for this project. No money is charged and the app is not connected to a payment processor.",
                    links: [{ label: "View payment history", href: "payments.html" }]
                },
                {
                    label: "When can I see a payment option?",
                    answer: "The demo payment flow is available after a charging session is completed. It records a simulated success or failure and does not charge money.",
                    links: [{ label: "View request status", href: "status.html" }, { label: "Payment history", href: "payments.html" }]
                },
                {
                    label: "Why is my session still active?",
                    answer: "Charging sessions are simulated. They complete automatically after the estimated charging duration. Refresh your status page to see the latest session state.",
                    links: [{ label: "Open status page", href: "status.html" }]
                }
            ]
        },
        {
            title: "Troubleshooting",
            topics: [
                {
                    label: "I cannot submit a booking",
                    answer: "Check that you have selected one of your vehicles, a station, and a charger. Your current battery must be between 0 and 100, and the target must be higher than the current level. The vehicle must have a valid battery capacity and a connector compatible with the charger.",
                    links: [{ label: "Review vehicles", href: "vehicle.html" }, { label: "Try booking again", href: "booking.html" }]
                },
                {
                    label: "My request or session does not look updated",
                    answer: "Open the request status page and refresh it to load the latest state. Pending requests may wait for a compatible charger. Once a session is active, its progress and completion are simulated by SmartCharge.",
                    links: [{ label: "Refresh request status", href: "status.html" }]
                },
                {
                    label: "A station or charger is not available",
                    answer: "Availability can change as chargers are assigned to sessions. Try another compatible charger or station, or check again later.",
                    links: [{ label: "Explore stations", href: "remote.html" }, { label: "Choose during booking", href: "booking.html" }]
                }
            ]
        }
    ];

    const launcher = document.createElement("button");
    launcher.type = "button";
    launcher.className = "sc-chat-launcher";
    launcher.setAttribute("aria-label", "Open SmartCharge Help chatbot");
    launcher.setAttribute("aria-controls", "smartchargeChatPanel");
    launcher.setAttribute("aria-expanded", "false");
    launcher.innerHTML = '<svg class="sc-chat-robot" viewBox="0 0 64 64" aria-hidden="true" focusable="false"><path class="sc-robot-antenna" d="M32 8v8"/><circle class="sc-robot-tip" cx="32" cy="6" r="3"/><rect class="sc-robot-head" x="12" y="17" width="40" height="34" rx="13"/><path class="sc-robot-ear" d="M8 29v10M56 29v10"/><circle class="sc-robot-eye" cx="24" cy="32" r="3"/><circle class="sc-robot-eye" cx="40" cy="32" r="3"/><path class="sc-robot-smile" d="M24 41c2.2 3 5 4 8 4s5.8-1 8-4"/><path class="sc-robot-cheek" d="M17 38h3M44 38h3"/></svg><span class="sc-chat-launcher-label">SmartCharge Help</span>';

    const panel = document.createElement("section");
    panel.id = "smartchargeChatPanel";
    panel.className = "sc-chat-panel";
    panel.setAttribute("role", "dialog");
    panel.setAttribute("aria-label", "SmartCharge guided support");
    panel.hidden = true;
    panel.innerHTML = `
        <header class="sc-chat-head">
            <div class="sc-chat-brand">
                <div class="sc-chat-brand-icon" aria-hidden="true">⚡</div>
                <div><div class="sc-chat-brand-title">SmartCharge Help</div><div class="sc-chat-brand-subtitle">Choose a topic to get started</div></div>
            </div>
            <button type="button" class="sc-chat-close" aria-label="Close help chat">×</button>
        </header>
        <div class="sc-chat-thread" aria-live="polite" aria-relevant="additions text"></div>
        <div class="sc-chat-status" hidden></div>
        <div class="sc-chat-choices" aria-label="Support options"></div>
        <div class="sc-chat-disclaimer">Guided answers from SmartCharge help. Choose an option below; free-text chat is not enabled.</div>`;

    document.body.append(launcher, panel);

    const thread = panel.querySelector(".sc-chat-thread");
    const choices = panel.querySelector(".sc-chat-choices");
    const status = panel.querySelector(".sc-chat-status");
    const closeButton = panel.querySelector(".sc-chat-close");
    let lastFocus = null;
    let hasStarted = false;

    function scrollToBottom() { thread.scrollTop = thread.scrollHeight; }

    function addMessage(text, kind = "bot", links = []) {
        const bubble = document.createElement("div");
        bubble.className = `sc-chat-message ${kind}`;
        bubble.textContent = text;
        if (links.length) {
            const linkRow = document.createElement("div");
            linkRow.className = "sc-chat-links";
            links.forEach(link => {
                const anchor = document.createElement("a");
                anchor.className = "sc-chat-link";
                anchor.href = link.href;
                anchor.textContent = link.label;
                linkRow.append(anchor);
            });
            bubble.append(linkRow);
        }
        thread.append(bubble);
        scrollToBottom();
        return bubble;
    }

    function renderChoices(items, { includeHome = false } = {}) {
        const hadChoiceFocus = choices.contains(document.activeElement);
        choices.replaceChildren();
        const options = [...items];
        if (includeHome) options.push({ label: "Main menu", action: showHome, primary: true });
        options.forEach(item => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = `sc-chat-choice${item.primary ? " primary" : ""}`;
            button.textContent = item.label;
            button.addEventListener("click", () => item.action());
            choices.append(button);
        });
        if (hadChoiceFocus) choices.querySelector("button")?.focus();
    }

    function showHome({ greet = false, fromCategory = null } = {}) {
        if (greet || !hasStarted) {
            addMessage("Hi! I can help with SmartCharge bookings, vehicles, stations, account access, and safety information. Choose a topic below.");
            hasStarted = true;
        } else if (fromCategory) {
            addMessage("What would you like to know about " + fromCategory.toLowerCase() + "?");
        }
        renderChoices(knowledge.map(category => ({
            label: category.title,
            action: () => showCategory(category)
        })));
        status.hidden = false;
        status.textContent = "Menu-based help · No free-text input";
    }

    function showCategory(category) {
        addMessage(category.title, "user");
        addMessage(`Here are the ${category.title.toLowerCase()} topics I can help with. Choose one to see the answer.`);
        renderChoices([...category.topics.map(topic => ({
            label: topic.label,
            action: () => showAnswer(category, topic)
        })), { label: "I need a different kind of help", action: showFallback }, { label: "Main menu", action: showHome, primary: true }]);
        status.hidden = true;
    }

    function showFallback() {
        addMessage("I don't have a prepared answer for that topic yet. Choose another support area or use the links below to continue in SmartCharge.");
        renderChoices([
            { label: "View my latest request", action: () => { addMessage("View my latest request", "user"); showLatestRequest(); } },
            { label: "Open booking", action: () => { window.location.href = "booking.html"; } },
            { label: "Main menu", action: showHome, primary: true }
        ]);
    }

    function showAnswer(category, topic) {
        addMessage(topic.label, "user");
        if (topic.action === "latestRequest") {
            showLatestRequest();
            return;
        }
        addMessage(topic.answer, "bot", topic.links || []);
        if (topic.urgent) status.textContent = "For immediate danger, contact local emergency services or qualified staff.";
        else status.textContent = "Need another answer? Choose a follow-up option.";
        status.hidden = false;
        renderChoices([
            { label: `More ${category.title.toLowerCase()}`, action: () => showCategory(category) },
            { label: "Main menu", action: showHome, primary: true }
        ]);
    }

    function readableStatus(value) {
        const descriptions = {
            PENDING: "Waiting in the queue for a compatible charger.",
            ALLOCATED: "A charger has been assigned; charging has not started yet.",
            ACTIVE: "The simulated charging session is in progress.",
            COMPLETED: "The charging session is complete.",
            CANCELLED: "The request was cancelled before charging began."
        };
        const key = String(value || "UNKNOWN").toUpperCase();
        return descriptions[key] || `Current status: ${key.toLowerCase().replaceAll("_", " ")}.`;
    }

    async function showLatestRequest() {
        const token = sessionStorage.getItem("smartchargeAccessToken");
        let user;
        try { user = JSON.parse(localStorage.getItem("smartchargeUser") || "null"); }
        catch { user = null; }

        if (!token || !user?.id) {
            addMessage("Sign in to check a request associated with your account. I can still answer general SmartCharge questions here.", "bot", [{ label: "Sign in", href: "login.html" }]);
            renderChoices([{ label: "Main menu", action: showHome, primary: true }]);
            return;
        }

        const loading = document.createElement("div");
        loading.className = "sc-chat-typing";
        loading.textContent = "Checking your latest request…";
        thread.append(loading);
        scrollToBottom();
        choices.replaceChildren();
        status.textContent = "Looking up your own request using your signed-in account.";
        status.hidden = false;

        try {
            const response = await fetch(`${API_BASE_URL}/charging/requests/user/${encodeURIComponent(user.id)}`, {
                headers: { "Accept": "application/json" }
            });
            loading.remove();
            if (response.status === 401 || response.status === 403) {
                addMessage("Your sign-in needs refreshing. Sign in again to check your request.", "bot", [{ label: "Sign in", href: "login.html" }]);
            } else if (!response.ok) {
                addMessage("I couldn't load your request right now. Please open your status page and try again.", "bot", [{ label: "Open status page", href: "status.html" }]);
            } else {
                const requests = await response.json();
                if (!Array.isArray(requests) || requests.length === 0) {
                    addMessage("I couldn't find a charging request on your account yet. You can create one from the booking page.", "bot", [{ label: "Create a booking", href: "booking.html" }]);
                } else {
                    const request = requests[0];
                    const when = request.arrivalTime ? new Date(request.arrivalTime).toLocaleString() : "time unavailable";
                    addMessage(`Your newest request is #${request.id}. ${readableStatus(request.status)}\nSubmitted: ${when}`, "bot", [
                        { label: "Open this request", href: `status.html?requestId=${encodeURIComponent(request.id)}` },
                        { label: "All my requests", href: "status.html" }
                    ]);
                }
            }
        } catch {
            loading.remove();
            addMessage("I couldn't reach SmartCharge just now. Please try again or open your status page.", "bot", [{ label: "Open status page", href: "status.html" }]);
        }
        status.textContent = "This lookup uses your signed-in account and only requests your own records.";
        status.hidden = false;
        renderChoices([{ label: "Charging request topics", action: () => showCategory(knowledge[0]) }, { label: "Main menu", action: showHome, primary: true }]);
    }

    function openChat() {
        lastFocus = document.activeElement;
        panel.hidden = false;
        launcher.setAttribute("aria-expanded", "true");
        if (!hasStarted) showHome({ greet: true });
        closeButton.focus();
    }

    function closeChat() {
        panel.hidden = true;
        launcher.setAttribute("aria-expanded", "false");
        (lastFocus && typeof lastFocus.focus === "function" ? lastFocus : launcher).focus();
    }

    launcher.addEventListener("click", () => panel.hidden ? openChat() : closeChat());
    closeButton.addEventListener("click", closeChat);
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !panel.hidden) closeChat();
    });
})();
