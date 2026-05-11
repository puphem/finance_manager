// ===================================================================================
// ==                            main.js (Версия 16.0 - Auth + JWT)                ==
// ===================================================================================

(function () {
    console.log("main.js: Скрипт начал выполняться.");

    const API_URL = '';

    const ICONS = ["fas fa-shopping-basket", "fas fa-car", "fas fa-taxi", "fas fa-film", "fas fa-file-invoice-dollar", "fas fa-utensils", "fas fa-house-user", "fas fa-gas-pump", "fas fa-plane", "fas fa-heart", "fas fa-gift", "fas fa-tshirt", "fas fa-paw", "fas fa-graduation-cap", "fas fa-spa", "fas fa-heartbeat"];
    const COLORS = ["#27ae60", "#2980b9", "#1f8a70", "#f39c12", "#c0392b", "#8e44ad", "#2c3e50", "#16a085", "#d35400", "#7f8c8d", "#e74c3c", "#34495e", "#e84393"];

    const TOKEN_KEY = 'finance_jwt_token';
    const USERNAME_KEY = 'finance_username';

    // =================== AUTH UTILITIES ===================

    const getToken = () => localStorage.getItem(TOKEN_KEY);
    const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
    const getStoredUsername = () => localStorage.getItem(USERNAME_KEY);
    const setStoredUsername = (username) => localStorage.setItem(USERNAME_KEY, username);

    const logout = () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        showAuth();
    };

    const showAuth = () => {
        document.getElementById('auth-page').style.display = 'flex';
        document.getElementById('app-page').style.display = 'none';
    };

    const showApp = () => {
        document.getElementById('auth-page').style.display = 'none';
        document.getElementById('app-page').style.display = 'block';
        const usernameEl = document.getElementById('current-username');
        if (usernameEl) usernameEl.textContent = getStoredUsername() || '';
    };

    // Wrapper for fetch with JWT header and 401 handling
    const apiFetch = async (url, options = {}) => {
        const token = getToken();
        const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
        if (token) headers['Authorization'] = `Bearer ${token}`;
        const response = await fetch(url, { ...options, headers });
        if (response.status === 401) {
            logout();
            throw new Error('Сессия истекла. Пожалуйста, войдите снова.');
        }
        return response;
    };

    // =================== AUTH PAGE SETUP ===================

    const setupAuthPage = () => {
        const tabLogin = document.getElementById('tab-login');
        const tabRegister = document.getElementById('tab-register');
        const loginForm = document.getElementById('login-form');
        const registerForm = document.getElementById('register-form');
        const loginError = document.getElementById('login-error');
        const registerError = document.getElementById('register-error');

        tabLogin.addEventListener('click', () => {
            tabLogin.classList.add('active');
            tabRegister.classList.remove('active');
            loginForm.style.display = '';
            registerForm.style.display = 'none';
            loginError.textContent = '';
        });

        tabRegister.addEventListener('click', () => {
            tabRegister.classList.add('active');
            tabLogin.classList.remove('active');
            registerForm.style.display = '';
            loginForm.style.display = 'none';
            registerError.textContent = '';
        });

        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            loginError.textContent = '';
            const username = document.getElementById('login-username').value;
            const password = document.getElementById('login-password').value;
            try {
                const response = await fetch(`${API_URL}/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password }),
                });
                if (response.ok) {
                    const data = await response.json();
                    setToken(data.token);
                    setStoredUsername(data.username);
                    showApp();
                    main().catch(err => console.error(err));
                } else {
                    const err = await response.json().catch(() => ({}));
                    loginError.textContent = err.message || 'Неверный логин или пароль.';
                }
            } catch (err) {
                loginError.textContent = 'Ошибка соединения с сервером.';
            }
        });

        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            registerError.textContent = '';
            const username = document.getElementById('reg-username').value;
            const password = document.getElementById('reg-password').value;
            try {
                const response = await fetch(`${API_URL}/auth/register`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password }),
                });
                if (response.status === 201) {
                    const data = await response.json();
                    setToken(data.token);
                    setStoredUsername(data.username);
                    showApp();
                    main().catch(err => console.error(err));
                } else {
                    const err = await response.json().catch(() => ({}));
                    registerError.textContent = err.message || 'Ошибка регистрации.';
                }
            } catch (err) {
                registerError.textContent = 'Ошибка соединения с сервером.';
            }
        });
    };

    // =================== MAIN APP ===================

    const getElement = (id) => {
        const element = document.getElementById(id);
        if (!element) throw new Error(`Критическая ошибка: Элемент с id="${id}" не найден!`);
        return element;
    };

    const getTodayDateString = () => {
        const now = new Date();
        const year = now.getFullYear();
        const month = (now.getMonth() + 1).toString().padStart(2, '0');
        const day = now.getDate().toString().padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    let appInitialized = false;

    async function main() {
        if (appInitialized) return;
        appInitialized = true;

        const transactionList = getElement('transaction-list');
        const filterButtons = getElement('filter-buttons');
        const absoluteBalanceEl = getElement('absolute-balance');
        const periodIncomeEl = getElement('period-income');
        const periodExpenseEl = getElement('period-expense');
        const periodNetEl = getElement('period-net');

        const addExpenseBtn = getElement('add-expense-btn');
        const scanReceiptBtn = getElement('scan-receipt-btn');
        const addIncomeBtn = getElement('add-income-btn');

        const expenseModal = getElement('expense-modal');
        const expenseForm = getElement('expense-form');
        const expenseCategorySelect = getElement('expense-category');
        const expenseErrorDiv = getElement('expense-error-message');
        const expenseDateInput = getElement('expense-date');
        const expenseIdInput = getElement('expense-id');
        const expenseModalTitle = getElement('expense-modal-title');
        const expenseDescriptionInput = getElement('expense-description');
        const expenseAmountInput = getElement('expense-amount');
        const expenseAmountHint = getElement('expense-amount-hint');

        const categoryModal = getElement('category-modal');
        const addCategoryLink = getElement('add-category-link');
        const categoryForm = getElement('category-form');
        const categoryErrorDiv = getElement('category-error-message');
        const iconPicker = getElement('icon-picker');
        const colorPalette = getElement('color-palette');
        const iconPreview = getElement('icon-preview');
        const categoryModalTitle = getElement('category-modal-title');
        const categoryIdInput = getElement('category-id');
        const categoryNameInput = getElement('category-name');

        const receiptModal = getElement('receipt-modal');
        const receiptFileInput = getElement('receipt-file-input');
        const receiptPreviewContainer = getElement('receipt-preview-container');
        const receiptPreview = getElement('receipt-preview');
        const receiptErrorDiv = getElement('receipt-error-message');

        const incomeModal = getElement('income-modal');
        const incomeForm = getElement('income-form');
        const incomeErrorDiv = getElement('income-error-message');
        const incomeDateInput = getElement('income-date');
        const incomeIdInput = getElement('income-id');
        const incomeModalTitle = getElement('income-modal-title');
        const incomeAmountInput = getElement('income-amount');
        const incomeDescriptionInput = getElement('income-description');

        const logoutBtn = getElement('logout-btn');

        const expenseChartCanvas = getElement('expense-chart');
        let expenseChart = null;
        let currentPeriod = 'month';

        let selectedIcon = ICONS[0];
        let selectedColor = COLORS[0];

        logoutBtn.addEventListener('click', logout);

        const renderPalettes = () => {
            iconPicker.innerHTML = ICONS.map(icon => `<div class="icon-option" data-icon="${icon}"><i class="${icon}"></i></div>`).join('');
            colorPalette.innerHTML = COLORS.map(color => `<div class="color-option" data-color="${color}" style="background-color: ${color};"></div>`).join('');
        };

        const updatePreview = () => {
            iconPreview.innerHTML = `<i class="${selectedIcon}"></i>`;
            iconPreview.style.color = selectedColor;
        };

        const fetchCategories = async () => {
            try {
                const response = await apiFetch(`${API_URL}/categories`);
                if (!response.ok) throw new Error('Не удалось загрузить категории');
                const categories = await response.json();

                const currentCategoryValue = expenseCategorySelect.value;
                expenseCategorySelect.innerHTML = '';
                categories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.id;
                    option.textContent = category.name;
                    expenseCategorySelect.appendChild(option);
                });
                if (currentCategoryValue) {
                    expenseCategorySelect.value = currentCategoryValue;
                }
            } catch (error) {
                expenseErrorDiv.textContent = error.message;
            }
        };

        const getCategoryIcon = (category) => {
            if (category?.icon) return category.icon;
            const categoryName = (category?.name || '').toLowerCase();
            if (categoryName.includes('продукт')) return 'fas fa-shopping-basket';
            if (categoryName.includes('транспорт')) return 'fas fa-bus';
            if (categoryName.includes('такси')) return 'fas fa-taxi';
            if (categoryName.includes('счет')) return 'fas fa-file-invoice-dollar';
            if (categoryName.includes('кафе') || categoryName.includes('ресторан')) return 'fas fa-utensils';
            if (categoryName.includes('развлеч')) return 'fas fa-film';
            if (categoryName.includes('одеж')) return 'fas fa-tshirt';
            if (categoryName.includes('здоров')) return 'fas fa-heartbeat';
            if (categoryName.includes('дом')) return 'fas fa-house-user';
            if (categoryName.includes('питом')) return 'fas fa-paw';
            return 'fas fa-receipt';
        };

        const renderExpenseChart = (summaryData) => {
            if (expenseChart) expenseChart.destroy();
            const labels = summaryData.map(item => item.categoryName);
            const data = summaryData.map(item => item.totalAmount);
            const colors = summaryData.map(item => item.categoryColor);
            expenseChart = new Chart(expenseChartCanvas, {
                type: 'doughnut',
                data: { labels, datasets: [{ label: 'Расходы по категориям', data, backgroundColor: colors, borderColor: '#fff', borderWidth: 2 }] },
                options: { responsive: true, plugins: { legend: { position: 'top' }, title: { display: true, text: 'Расходы по категориям' } } }
            });
        };

        const updateDashboard = async (period = currentPeriod) => {
            currentPeriod = period;
            try {
                const summaryRes = await apiFetch(`${API_URL}/balance/summary?period=${period}`);
                if (!summaryRes.ok) throw new Error('Не удалось загрузить сводку');
                const summary = await summaryRes.json();

                absoluteBalanceEl.textContent = `${summary.absoluteBalance.toFixed(2)} руб.`;
                periodIncomeEl.innerHTML = `Доход<br><span style="color: green; font-weight: bold;">+${summary.totalIncomeForPeriod.toFixed(2)}</span>`;
                periodExpenseEl.innerHTML = `Расход<br><span style="color: red; font-weight: bold;">-${summary.totalExpenseForPeriod.toFixed(2)}</span>`;
                periodNetEl.innerHTML = `Итог<br><span style="font-weight: bold;">${summary.netPeriodResult.toFixed(2)}</span>`;

                const chartRes = await apiFetch(`${API_URL}/expenses/summary-by-category?period=${period}`);
                if (!chartRes.ok) throw new Error('Не удалось загрузить данные для диаграммы');
                renderExpenseChart(await chartRes.json());

                const [expenseRes, incomeRes] = await Promise.all([
                    apiFetch(`${API_URL}/expenses?period=${period}`),
                    apiFetch(`${API_URL}/incomes?period=${period}`)
                ]);
                if (!expenseRes.ok) throw new Error(`Ошибка загрузки расходов`);
                if (!incomeRes.ok) throw new Error(`Ошибка загрузки доходов`);

                const expenses = await expenseRes.json();
                const incomes = await incomeRes.json();
                expenses.forEach(e => e.transactionType = 'expense');
                incomes.forEach(i => i.transactionType = 'income');

                const allTransactions = [...expenses, ...incomes].sort((a, b) => new Date(b.date) - new Date(a.date));

                transactionList.innerHTML = '';
                allTransactions.forEach(tx => {
                    const listItem = document.createElement('li');
                    const date = new Date(tx.date).toLocaleDateString('ru-RU');
                    let actions = '';
                    if (tx.transactionType === 'expense') {
                        const btns = [`<button class="edit-btn" data-id="${tx.id}" data-type="expense">✏️</button>`];
                        if (!tx.receipt) btns.push(`<button class="delete-btn" data-id="${tx.id}" data-type="expense">🗑️</button>`);
                        actions = `<div class="transaction-actions">${btns.join('')}</div>`;
                    } else {
                        actions = `<div class="transaction-actions"><button class="edit-btn" data-id="${tx.id}" data-type="income">✏️</button><button class="delete-btn" data-id="${tx.id}" data-type="income">🗑️</button></div>`;
                    }
                    if (tx.transactionType === 'expense') {
                        listItem.innerHTML = `
                            <div style="font-size: 1.5em; margin-right: 15px; color: ${tx.category.color || '#7f8c8d'};"><i class="${getCategoryIcon(tx.category)}"></i></div>
                            <div style="width: 100%; display: flex; justify-content: space-between;">
                                <span><strong>-${tx.amount.toFixed(2)} руб.</strong> - ${tx.description || tx.category.name}</span>
                                <small style="color: #555;">${date}</small>
                            </div>
                            ${actions}`;
                    } else {
                        listItem.style.color = 'green';
                        listItem.innerHTML = `
                            <div style="font-size: 1.5em; margin-right: 15px;"><i class="fas fa-money-bill-wave"></i></div>
                            <div style="width: 100%; display: flex; justify-content: space-between;">
                                <span><strong>+${tx.amount.toFixed(2)} руб.</strong> - ${tx.description || 'Доход'}</span>
                                <small style="color: #555;">${date}</small>
                            </div>
                            ${actions}`;
                    }
                    transactionList.appendChild(listItem);
                });
            } catch (error) {
                console.error("Ошибка при обновлении дашборда:", error);
            }
        };

        const sendQrDataToServer = async (qrData) => {
            try {
                const response = await apiFetch(`${API_URL}/receipts/scan`, {
                    method: 'POST',
                    body: JSON.stringify({ qrCodeData: qrData }),
                });
                if (response.status === 201) {
                    const scannedReceipt = await response.json();
                    const scannedExpense = scannedReceipt.expenses && scannedReceipt.expenses.length > 0 ? scannedReceipt.expenses[0] : null;
                    if (!scannedExpense) throw new Error('Сервер не вернул расход из чека.');

                    receiptModal.style.display = 'none';
                    await updateDashboard(currentPeriod);

                    expenseModalTitle.textContent = 'Проверьте расход из чека';
                    expenseIdInput.value = scannedExpense.id;
                    expenseAmountInput.value = scannedExpense.amount;
                    expenseDateInput.value = scannedExpense.date;
                    expenseDescriptionInput.value = scannedExpense.description || '';
                    if (scannedExpense.category?.id) expenseCategorySelect.value = scannedExpense.category.id;
                    expenseAmountInput.readOnly = true;
                    expenseAmountHint.classList.remove('hidden');
                    expenseErrorDiv.textContent = '';
                    expenseModal.style.display = 'block';
                } else {
                    const errorData = await response.json().catch(() => ({}));
                    receiptErrorDiv.textContent = `Ошибка сервера: ${errorData.message || 'Неизвестная ошибка'}`;
                }
            } catch (error) {
                receiptErrorDiv.textContent = `Ошибка: ${error.message}`;
            }
        };

        addExpenseBtn.addEventListener('click', () => {
            expenseModalTitle.textContent = 'Добавить новый расход';
            expenseIdInput.value = '';
            expenseErrorDiv.textContent = '';
            expenseForm.reset();
            expenseDateInput.value = getTodayDateString();
            expenseAmountInput.readOnly = false;
            expenseAmountHint.classList.add('hidden');
            expenseModal.style.display = 'block';
        });

        addIncomeBtn.addEventListener('click', () => {
            incomeModalTitle.textContent = 'Добавить новый доход';
            incomeIdInput.value = '';
            incomeErrorDiv.textContent = '';
            incomeForm.reset();
            incomeDateInput.value = getTodayDateString();
            incomeModal.style.display = 'block';
        });

        addCategoryLink.addEventListener('click', (e) => {
            e.preventDefault();
            categoryModalTitle.textContent = 'Создать новую категорию';
            categoryIdInput.value = '';
            categoryErrorDiv.textContent = '';
            categoryForm.reset();
            selectedIcon = ICONS[0];
            selectedColor = COLORS[0];
            updatePreview();
            categoryModal.style.display = 'block';
        });

        scanReceiptBtn.addEventListener('click', () => {
            receiptErrorDiv.textContent = '';
            receiptFileInput.value = '';
            receiptPreviewContainer.style.display = 'none';
            receiptModal.style.display = 'block';
        });

        document.querySelectorAll('.close-button').forEach(btn => {
            btn.onclick = () => { btn.closest('.modal').style.display = 'none'; };
        });

        window.onclick = (event) => {
            if (event.target.classList.contains('modal')) event.target.style.display = 'none';
        };

        expenseForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            expenseErrorDiv.textContent = '';
            const id = expenseIdInput.value;
            const url = id ? `${API_URL}/expenses/${id}` : `${API_URL}/expenses`;
            const method = id ? 'PUT' : 'POST';
            try {
                const response = await apiFetch(url, {
                    method,
                    body: JSON.stringify({
                        amount: expenseAmountInput.value,
                        categoryId: expenseCategorySelect.value,
                        description: expenseDescriptionInput.value,
                        date: expenseDateInput.value
                    }),
                });
                if (response.ok) {
                    expenseModal.style.display = 'none';
                    await updateDashboard(currentPeriod);
                } else {
                    const errorData = await response.json().catch(() => ({}));
                    expenseErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                }
            } catch (error) {
                expenseErrorDiv.textContent = `Ошибка: ${error.message}`;
            }
        });

        incomeForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            incomeErrorDiv.textContent = '';
            const id = incomeIdInput.value;
            const url = id ? `${API_URL}/incomes/${id}` : `${API_URL}/incomes`;
            const method = id ? 'PUT' : 'POST';
            try {
                const response = await apiFetch(url, {
                    method,
                    body: JSON.stringify({
                        amount: incomeAmountInput.value,
                        description: incomeDescriptionInput.value,
                        date: incomeDateInput.value
                    }),
                });
                if (response.ok) {
                    incomeModal.style.display = 'none';
                    await updateDashboard(currentPeriod);
                } else {
                    const errorData = await response.json().catch(() => ({}));
                    incomeErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                }
            } catch (error) {
                incomeErrorDiv.textContent = `Ошибка: ${error.message}`;
            }
        });

        categoryForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            categoryErrorDiv.textContent = '';
            try {
                const response = await apiFetch(`${API_URL}/categories`, {
                    method: 'POST',
                    body: JSON.stringify({ name: categoryNameInput.value, color: selectedColor, icon: selectedIcon }),
                });
                if (response.status === 201) {
                    categoryModal.style.display = 'none';
                    await fetchCategories();
                } else {
                    const errorData = await response.json().catch(() => ({}));
                    categoryErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                }
            } catch (error) {
                categoryErrorDiv.textContent = `Ошибка: ${error.message}`;
            }
        });

        receiptFileInput.addEventListener('change', (event) => {
            const file = event.target.files[0];
            if (!file) return;
            receiptErrorDiv.textContent = 'Обработка...';
            receiptPreview.src = URL.createObjectURL(file);
            receiptPreviewContainer.style.display = 'block';
            const reader = new FileReader();
            reader.onload = (e) => {
                const img = new Image();
                img.onload = () => {
                    const canvas = document.createElement('canvas');
                    const context = canvas.getContext('2d', { willReadFrequently: true });
                    canvas.width = img.width;
                    canvas.height = img.height;
                    context.drawImage(img, 0, 0);
                    const imageData = context.getImageData(0, 0, img.width, img.height);
                    const code = jsQR(imageData.data, imageData.width, imageData.height);
                    if (code) {
                        receiptErrorDiv.textContent = 'QR-код найден! Отправка на сервер...';
                        sendQrDataToServer(code.data);
                    } else {
                        receiptErrorDiv.textContent = 'Ошибка: QR-код не найден на изображении.';
                    }
                };
                img.onerror = () => { receiptErrorDiv.textContent = 'Ошибка: Не удалось прочитать файл как изображение.'; };
                img.src = e.target.result;
            };
            reader.onerror = () => { receiptErrorDiv.textContent = 'Ошибка: Не удалось прочитать файл.'; };
            reader.readAsDataURL(file);
        });

        filterButtons.addEventListener('click', (event) => {
            if (event.target.tagName === 'BUTTON') {
                filterButtons.querySelectorAll('button').forEach(btn => btn.classList.remove('active'));
                event.target.classList.add('active');
                const period = event.target.dataset.period;
                currentPeriod = period;
                updateDashboard(period);
            }
        });

        transactionList.addEventListener('click', async e => {
            const target = e.target.closest('button');
            if (!target) return;
            const id = target.dataset.id;
            const type = target.dataset.type;

            if (target.classList.contains('delete-btn')) {
                if (confirm('Вы уверены, что хотите удалить эту запись?')) {
                    await apiFetch(`${API_URL}/${type}s/${id}`, { method: 'DELETE' });
                    await updateDashboard(currentPeriod);
                }
            }

            if (target.classList.contains('edit-btn')) {
                const response = await apiFetch(`${API_URL}/${type}s/${id}`);
                const data = await response.json();
                if (type === 'expense') {
                    expenseModalTitle.textContent = 'Редактировать расход';
                    expenseIdInput.value = data.id;
                    expenseAmountInput.value = data.amount;
                    expenseDateInput.value = data.date;
                    expenseDescriptionInput.value = data.description;
                    expenseCategorySelect.value = data.category.id;
                    const isReceiptExpense = !!data.receipt;
                    expenseAmountInput.readOnly = isReceiptExpense;
                    expenseAmountHint.classList.toggle('hidden', !isReceiptExpense);
                    expenseModal.style.display = 'block';
                } else if (type === 'income') {
                    incomeModalTitle.textContent = 'Редактировать доход';
                    incomeIdInput.value = data.id;
                    incomeAmountInput.value = data.amount;
                    incomeDateInput.value = data.date;
                    incomeDescriptionInput.value = data.description;
                    incomeModal.style.display = 'block';
                }
            }
        });

        iconPicker.addEventListener('click', e => {
            const target = e.target.closest('.icon-option');
            if (!target) return;
            selectedIcon = target.dataset.icon;
            document.querySelectorAll('.icon-option').forEach(opt => opt.classList.remove('selected'));
            target.classList.add('selected');
            updatePreview();
        });

        colorPalette.addEventListener('click', e => {
            const target = e.target.closest('.color-option');
            if (!target) return;
            selectedColor = target.dataset.color;
            document.querySelectorAll('.color-option').forEach(opt => opt.classList.remove('selected'));
            target.classList.add('selected');
            updatePreview();
        });

        renderPalettes();
        filterButtons.querySelectorAll('button').forEach(btn => btn.classList.toggle('active', btn.dataset.period === currentPeriod));
        await fetchCategories();
        await updateDashboard(currentPeriod);
        console.log("main.js: Начальная загрузка данных завершена.");
    }

    // =================== INIT ===================

    setupAuthPage();

    if (getToken()) {
        showApp();
        main().catch(err => console.error("Ошибка инициализации приложения:", err));
    } else {
        showAuth();
    }

})();
