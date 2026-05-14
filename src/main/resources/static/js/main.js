(function () {
    console.log('main.js: Скрипт начал выполняться.');

    const API_URL = '';

    const ICONS = ['fas fa-shopping-basket', 'fas fa-car', 'fas fa-taxi', 'fas fa-film', 'fas fa-file-invoice-dollar', 'fas fa-utensils', 'fas fa-house-user', 'fas fa-gas-pump', 'fas fa-plane', 'fas fa-heart', 'fas fa-gift', 'fas fa-tshirt', 'fas fa-paw', 'fas fa-graduation-cap', 'fas fa-spa', 'fas fa-heartbeat'];
    const COLORS = ['#27ae60', '#2980b9', '#1f8a70', '#f39c12', '#c0392b', '#8e44ad', '#2c3e50', '#16a085', '#d35400', '#7f8c8d', '#e74c3c', '#34495e', '#e84393'];

    const TOKEN_KEY = 'finance_jwt_token';
    const USERNAME_KEY = 'finance_username';
    const THEME_KEY = 'finance_theme';
    const FONT_SIZE_KEY = 'finance_font_size';
    const RECENT_EXPENSE_LIMIT = 7;
    const EXPENSES_PAGE_STEP = 20;

    const getToken = () => localStorage.getItem(TOKEN_KEY);
    const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
    const getStoredUsername = () => localStorage.getItem(USERNAME_KEY);
    const setStoredUsername = (username) => localStorage.setItem(USERNAME_KEY, username);

    const logout = () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        window.location.reload();
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

    const applyTheme = (theme) => {
        const nextTheme = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.dataset.theme = nextTheme;
        localStorage.setItem(THEME_KEY, nextTheme);
    };

    const applyFontSize = (size) => {
        const allowed = new Set(['small', 'normal', 'large']);
        const nextSize = allowed.has(size) ? size : 'normal';
        document.documentElement.dataset.fontSize = nextSize;
        localStorage.setItem(FONT_SIZE_KEY, nextSize);
    };

    const registerPwaSupport = async () => {
        if (!('serviceWorker' in navigator)) return;
        try {
            await navigator.serviceWorker.register('/service-worker.js');
        } catch (error) {
            console.warn('Service Worker не зарегистрирован:', error);
        }
    };

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

    const getElement = (id) => {
        const element = document.getElementById(id);
        if (!element) throw new Error(`Критическая ошибка: Элемент с id="${id}" не найден!`);
        return element;
    };

    const toDateInputValue = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const getTodayDateString = () => toDateInputValue(new Date());

    const parseHexColor = (hex) => {
        if (!hex) return null;
        const normalized = hex.trim().replace('#', '');
        if (normalized.length === 3) {
            const [r, g, b] = normalized.split('').map(c => parseInt(c + c, 16));
            return Number.isNaN(r) ? null : { r, g, b };
        }
        if (normalized.length !== 6) return null;
        const r = parseInt(normalized.slice(0, 2), 16);
        const g = parseInt(normalized.slice(2, 4), 16);
        const b = parseInt(normalized.slice(4, 6), 16);
        if ([r, g, b].some(Number.isNaN)) return null;
        return { r, g, b };
    };

    const rgbToHex = ({ r, g, b }) => {
        const toHex = (value) => value.toString(16).padStart(2, '0');
        return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
    };

    const adjustHexColor = (hex, amount) => {
        const rgb = parseHexColor(hex);
        if (!rgb) return '#95a5a6';
        const clamp = (value) => Math.max(0, Math.min(255, value));
        return rgbToHex({
            r: clamp(rgb.r + amount),
            g: clamp(rgb.g + amount),
            b: clamp(rgb.b + amount),
        });
    };

    const escapeHtml = (value) => String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');

    let appInitialized = false;
    let initInProgress = false;

    async function main() {
        if (appInitialized || initInProgress) return;
        initInProgress = true;

        try {
            const navButtons = Array.from(document.querySelectorAll('.app-nav-btn'));
            const appViews = Array.from(document.querySelectorAll('.app-view'));

            const absoluteBalanceEl = getElement('absolute-balance');
            const periodIncomeEl = getElement('period-income');
            const periodExpenseEl = getElement('period-expense');
            const periodNetEl = getElement('period-net');
            const dashboardErrorDiv = getElement('dashboard-error-message');

            const addExpenseBtn = getElement('add-expense-btn');
            const scanReceiptBtn = getElement('scan-receipt-btn');
            const addIncomeBtn = getElement('add-income-btn');

            const chartFilterButtons = getElement('chart-filter-buttons');
            const expenseChartCanvas = getElement('expense-chart');
            const recentExpenseList = getElement('recent-expense-list');
            const openAllExpensesBtn = getElement('open-all-expenses-btn');
            const backToDashboardBtn = getElement('back-to-dashboard-btn');

            const expensesPageList = getElement('expenses-page-list');
            const expensesPageSummary = getElement('expenses-page-summary');
            const expensesPageLoadMoreBtn = getElement('expenses-page-load-more-btn');
            const expensesFilterPresets = getElement('expenses-filter-presets');
            const expensesFilterStart = getElement('expenses-filter-start');
            const expensesFilterEnd = getElement('expenses-filter-end');
            const expensesFilterCategory = getElement('expenses-filter-category');
            const expensesFilterSearch = getElement('expenses-filter-search');
            const applyExpensesFiltersBtn = getElement('apply-expenses-filters-btn');
            const resetExpensesFiltersBtn = getElement('reset-expenses-filters-btn');

            const fontSizeSelect = getElement('font-size-select');
            const darkThemeToggle = getElement('dark-theme-toggle');

            const expenseModal = getElement('expense-modal');
            const expenseForm = getElement('expense-form');
            const expenseCategorySelect = getElement('expense-category');
            const expenseSubcategorySelect = getElement('expense-subcategory');
            const newSubcategoryNameInput = getElement('new-subcategory-name');
            const addSubcategoryBtn = getElement('add-subcategory-btn');
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
            logoutBtn.addEventListener('click', logout);

            const initialTheme = localStorage.getItem(THEME_KEY) || 'light';
            const initialFontSize = localStorage.getItem(FONT_SIZE_KEY) || 'normal';
            applyTheme(initialTheme);
            applyFontSize(initialFontSize);
            darkThemeToggle.checked = initialTheme === 'dark';
            fontSizeSelect.value = initialFontSize;

            darkThemeToggle.addEventListener('change', () => {
                applyTheme(darkThemeToggle.checked ? 'dark' : 'light');
            });

            fontSizeSelect.addEventListener('change', () => {
                applyFontSize(fontSizeSelect.value);
            });

            const setActiveView = (viewId) => {
                appViews.forEach(view => {
                    view.classList.toggle('hidden', view.id !== viewId);
                });
                navButtons.forEach(btn => {
                    btn.classList.toggle('active', btn.dataset.view === viewId);
                });
            };

            navButtons.forEach(btn => {
                btn.addEventListener('click', () => setActiveView(btn.dataset.view));
            });

            openAllExpensesBtn.addEventListener('click', () => setActiveView('expenses-page-view'));
            backToDashboardBtn.addEventListener('click', () => setActiveView('dashboard-view'));

            let selectedIcon = ICONS[0];
            let selectedColor = COLORS[0];
            let currentChartPeriod = 'month';
            let currentCategorySummary = [];
            let selectedChartMode = 'category';
            let selectedChartCategoryId = null;
            let selectedChartCategoryName = '';
            let categoryById = new Map();
            let categoryNameById = new Map();
            let allExpensesCache = [];
            let filteredExpensesPage = [];
            let expensesVisibleCount = EXPENSES_PAGE_STEP;
            let expenseChart = null;
            let expensesPagePreset = 'month';

            const renderPalettes = () => {
                iconPicker.innerHTML = ICONS.map(icon => `<div class="icon-option" data-icon="${icon}"><i class="${icon}"></i></div>`).join('');
                colorPalette.innerHTML = COLORS.map(color => `<div class="color-option" data-color="${color}" style="background-color: ${color};"></div>`).join('');
            };

            const updatePreview = () => {
                iconPreview.innerHTML = `<i class="${selectedIcon}"></i>`;
                iconPreview.style.color = selectedColor;
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

            const populateSubcategories = (selectedSubcategoryId = '') => {
                const selectedCategory = categoryById.get(Number(expenseCategorySelect.value));
                const subcategories = selectedCategory?.subcategories || [];

                expenseSubcategorySelect.innerHTML = '';
                const emptyOption = document.createElement('option');
                emptyOption.value = '';
                emptyOption.textContent = 'Без подкатегории';
                expenseSubcategorySelect.appendChild(emptyOption);

                subcategories.forEach(subcategory => {
                    const option = document.createElement('option');
                    option.value = subcategory.id;
                    option.textContent = subcategory.name;
                    expenseSubcategorySelect.appendChild(option);
                });

                expenseSubcategorySelect.value = selectedSubcategoryId ? String(selectedSubcategoryId) : '';
            };

            const populateExpenseCategoryFilter = () => {
                const currentValue = expensesFilterCategory.value;
                expensesFilterCategory.innerHTML = '';
                const allOption = document.createElement('option');
                allOption.value = '';
                allOption.textContent = 'Все категории';
                expensesFilterCategory.appendChild(allOption);

                Array.from(categoryById.values()).forEach(category => {
                    const option = document.createElement('option');
                    option.value = String(category.id);
                    option.textContent = category.name;
                    expensesFilterCategory.appendChild(option);
                });

                if (currentValue) expensesFilterCategory.value = currentValue;
            };

            const fetchCategories = async () => {
                const response = await apiFetch(`${API_URL}/categories`);
                if (!response.ok) throw new Error('Не удалось загрузить категории');
                const categories = await response.json();
                categoryById = new Map(categories.map(category => [Number(category.id), category]));
                categoryNameById = new Map(categories.map(category => [Number(category.id), category.name]));

                const currentCategoryValue = expenseCategorySelect.value;
                expenseCategorySelect.innerHTML = '';
                categories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.id;
                    option.textContent = category.name;
                    expenseCategorySelect.appendChild(option);
                });
                if (currentCategoryValue) expenseCategorySelect.value = currentCategoryValue;
                populateSubcategories();
                populateExpenseCategoryFilter();
            };

            const createSubcategoryQuickly = async () => {
                const name = (newSubcategoryNameInput.value || '').trim();
                const categoryId = expenseCategorySelect.value;
                if (!categoryId) {
                    expenseErrorDiv.textContent = 'Сначала выберите категорию.';
                    return;
                }
                if (!name) {
                    expenseErrorDiv.textContent = 'Введите название подкатегории.';
                    return;
                }

                const response = await apiFetch(`${API_URL}/categories/subcategories`, {
                    method: 'POST',
                    body: JSON.stringify({ name, categoryId: Number(categoryId) }),
                });
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    throw new Error(errorData.message || 'Не удалось создать подкатегорию');
                }

                const createdSubcategory = await response.json();
                newSubcategoryNameInput.value = '';
                await fetchCategories();
                populateSubcategories(createdSubcategory.id);
            };

            const applyPresetDates = (preset) => {
                const today = new Date();
                const end = new Date(today);
                let start = null;

                if (preset === 'today') {
                    start = new Date(today);
                } else if (preset === 'yesterday') {
                    start = new Date(today);
                    start.setDate(start.getDate() - 1);
                    end.setDate(end.getDate() - 1);
                } else if (preset === 'week') {
                    start = new Date(today);
                    start.setDate(start.getDate() - 6);
                } else if (preset === 'month') {
                    start = new Date(today);
                    start.setDate(start.getDate() - 29);
                } else if (preset === 'custom') {
                    if (!expensesFilterStart.value || !expensesFilterEnd.value) {
                        expensesFilterStart.value = toDateInputValue(new Date(today.getFullYear(), today.getMonth(), 1));
                        expensesFilterEnd.value = toDateInputValue(today);
                    }
                    return;
                } else {
                    expensesFilterStart.value = '';
                    expensesFilterEnd.value = '';
                    return;
                }

                expensesFilterStart.value = toDateInputValue(start);
                expensesFilterEnd.value = toDateInputValue(end);
            };

            const getSubcategoryTint = (categoryColor, index = 0) => {
                const shifts = [-30, -12, 10, 24, 36, -22, 18, 30];
                const shift = shifts[index % shifts.length];
                return adjustHexColor(categoryColor || '#95a5a6', shift);
            };

            const renderExpenseRow = (expense, index) => {
                const item = document.createElement('li');
                const date = new Date(expense.date).toLocaleDateString('ru-RU');
            const category = expense.category || {};
            const icon = getCategoryIcon(category);
            const amountText = `-${Number(expense.amount || 0).toFixed(2)} руб.`;
            const safeDescription = escapeHtml(expense.description || category.name || 'Расход');
            const safeSubcategory = escapeHtml(expense.subcategory?.name || '');

            const subcategoryBadge = expense.subcategory?.name
                ? `<span class="transaction-subcategory" style="background:${getSubcategoryTint(category.color, index)}">${safeSubcategory}</span>`
                : '';

            item.innerHTML = `
                    <div style="font-size:1.35em; color:${category.color || '#7f8c8d'};"><i class="${icon}"></i></div>
                    <div class="transaction-main">
                        <div class="transaction-title">
                            <span><strong>${amountText}</strong> — ${safeDescription}</span>
                            ${subcategoryBadge}
                        </div>
                        <small class="transaction-date">${date}</small>
                    </div>
                    <div class="transaction-actions">
                        <button class="edit-btn" data-id="${expense.id}" data-type="expense">✏️</button>
                        <button class="delete-btn" data-id="${expense.id}" data-type="expense">🗑️</button>
                    </div>`;
                return item;
            };

            const renderRecentExpenses = () => {
                recentExpenseList.innerHTML = '';
                const recent = allExpensesCache.slice(0, RECENT_EXPENSE_LIMIT);
                if (recent.length === 0) {
                    recentExpenseList.innerHTML = '<li>Пока нет трат.</li>';
                    return;
                }
                recent.forEach((expense, index) => recentExpenseList.appendChild(renderExpenseRow(expense, index)));
            };

            const renderExpensesPage = () => {
                expensesPageList.innerHTML = '';
                const visible = filteredExpensesPage.slice(0, expensesVisibleCount);
                visible.forEach((expense, index) => expensesPageList.appendChild(renderExpenseRow(expense, index)));

                expensesPageSummary.textContent = `Найдено: ${filteredExpensesPage.length}`;
                expensesPageLoadMoreBtn.classList.toggle('hidden', filteredExpensesPage.length <= expensesVisibleCount);
            };

            const filterExpensesPage = () => {
                const selectedCategoryId = expensesFilterCategory.value ? Number(expensesFilterCategory.value) : null;
                const query = (expensesFilterSearch.value || '').trim().toLowerCase();
                const startDate = expensesFilterStart.value ? new Date(`${expensesFilterStart.value}T00:00:00`) : null;
                const endDate = expensesFilterEnd.value ? new Date(`${expensesFilterEnd.value}T23:59:59`) : null;

                filteredExpensesPage = allExpensesCache.filter(expense => {
                    const expenseDate = new Date(expense.date);
                    if (startDate && expenseDate < startDate) return false;
                    if (endDate && expenseDate > endDate) return false;
                    if (selectedCategoryId && Number(expense.category?.id) !== selectedCategoryId) return false;

                    if (query) {
                        const haystack = [
                            expense.description || '',
                            expense.category?.name || '',
                            expense.subcategory?.name || '',
                        ].join(' ').toLowerCase();
                        if (!haystack.includes(query)) return false;
                    }
                    return true;
                });

                expensesVisibleCount = EXPENSES_PAGE_STEP;
                renderExpensesPage();
            };

            const refreshAllExpenses = async () => {
                const response = await apiFetch(`${API_URL}/expenses?period=all`);
                if (!response.ok) throw new Error('Не удалось загрузить траты');
                const expenses = await response.json();
                allExpensesCache = expenses.sort((a, b) => new Date(b.date) - new Date(a.date));
                renderRecentExpenses();
                filterExpensesPage();
            };

            const buildSubcategoryColors = (summaryData) => {
                return summaryData.map((item, index) => getSubcategoryTint(item.categoryColor || '#95a5a6', index));
            };

            const renderExpenseChart = (summaryData, mode, titleText) => {
                if (expenseChart) expenseChart.destroy();
                selectedChartMode = mode;
                const labels = summaryData.map(item => mode === 'category' ? item.categoryName : item.subcategoryName);
                const data = summaryData.map(item => item.totalAmount);
                const colors = mode === 'category'
                    ? summaryData.map(item => item.categoryColor || '#95a5a6')
                    : buildSubcategoryColors(summaryData);

                expenseChart = new Chart(expenseChartCanvas, {
                    type: 'doughnut',
                    data: {
                        labels,
                        datasets: [{ label: 'Расходы', data, backgroundColor: colors, borderColor: '#fff', borderWidth: 2 }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: { position: 'top' },
                            title: { display: true, text: titleText }
                        },
                        onClick: async (_, elements) => {
                            if (mode === 'category' && elements.length > 0) {
                                const item = summaryData[elements[0].index];
                                if (!item?.categoryId) return;
                                selectedChartCategoryId = Number(item.categoryId);
                                selectedChartCategoryName = categoryNameById.get(Number(item.categoryId)) || item.categoryName || '';
                                const response = await apiFetch(`${API_URL}/expenses/summary-by-subcategory?categoryId=${item.categoryId}&period=${currentChartPeriod}`);
                                if (!response.ok) return;
                                const subcategorySummary = await response.json();
                                renderExpenseChart(subcategorySummary, 'subcategory', `Подкатегории: ${selectedChartCategoryName}`);
                                return;
                            }

                            if (mode === 'subcategory' && elements.length === 0) {
                                selectedChartCategoryId = null;
                                selectedChartCategoryName = '';
                                renderExpenseChart(currentCategorySummary, 'category', 'Расходы по категориям');
                            }
                        }
                    },
                    plugins: [{
                        id: 'centerReturnHint',
                        afterDraw(chart) {
                            if (mode !== 'subcategory') return;
                            const { ctx, chartArea: { left, right, top, bottom } } = chart;
                            const centerX = (left + right) / 2;
                            const centerY = (top + bottom) / 2;
                            ctx.save();
                            ctx.font = '600 14px Arial';
                            ctx.fillStyle = '#666';
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillText('← Назад', centerX, centerY);
                            ctx.restore();
                        }
                    }]
                });
            };

            const updateDashboard = async () => {
                dashboardErrorDiv.textContent = '';
                try {
                    const [summaryRes, chartRes] = await Promise.all([
                        apiFetch(`${API_URL}/balance/summary?period=month`),
                        apiFetch(`${API_URL}/expenses/summary-by-category?period=${currentChartPeriod}`),
                    ]);

                    if (!summaryRes.ok) throw new Error('Не удалось загрузить сводку.');
                    if (!chartRes.ok) throw new Error('Не удалось загрузить данные диаграммы.');

                    const summary = await summaryRes.json();
                    currentCategorySummary = await chartRes.json();

                    absoluteBalanceEl.textContent = `${Number(summary.absoluteBalance || 0).toFixed(2)} руб.`;
                    periodIncomeEl.innerHTML = `Доход (месяц)<br><span style="color: var(--success); font-weight: bold;">+${Number(summary.totalIncomeForPeriod || 0).toFixed(2)}</span>`;
                    periodExpenseEl.innerHTML = `Расход (месяц)<br><span style="color: var(--danger); font-weight: bold;">-${Number(summary.totalExpenseForPeriod || 0).toFixed(2)}</span>`;
                    periodNetEl.innerHTML = `Итог (месяц)<br><span style="font-weight: bold;">${Number(summary.netPeriodResult || 0).toFixed(2)}</span>`;

                    if (selectedChartMode === 'subcategory' && selectedChartCategoryId) {
                        const subcategoryRes = await apiFetch(`${API_URL}/expenses/summary-by-subcategory?categoryId=${selectedChartCategoryId}&period=${currentChartPeriod}`);
                        if (subcategoryRes.ok) {
                            const subcategorySummary = await subcategoryRes.json();
                            renderExpenseChart(subcategorySummary, 'subcategory', `Подкатегории: ${selectedChartCategoryName || 'категория'}`);
                        } else {
                            selectedChartCategoryId = null;
                            selectedChartCategoryName = '';
                            renderExpenseChart(currentCategorySummary, 'category', 'Расходы по категориям');
                        }
                    } else {
                        renderExpenseChart(currentCategorySummary, 'category', 'Расходы по категориям');
                    }

                    await refreshAllExpenses();
                } catch (error) {
                    console.error('Ошибка обновления дашборда:', error);
                    dashboardErrorDiv.textContent = error.message || 'Ошибка загрузки данных. Попробуйте обновить страницу.';
                    absoluteBalanceEl.textContent = 'Ошибка загрузки';
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
                        await updateDashboard();

                        expenseModalTitle.textContent = 'Проверьте расход из чека';
                        expenseIdInput.value = scannedExpense.id;
                        expenseAmountInput.value = scannedExpense.amount;
                        expenseDateInput.value = scannedExpense.date;
                        expenseDescriptionInput.value = scannedExpense.description || '';
                        if (scannedExpense.category?.id) expenseCategorySelect.value = scannedExpense.category.id;
                        populateSubcategories(scannedExpense.subcategory?.id);
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
                newSubcategoryNameInput.value = '';
                expenseDateInput.value = getTodayDateString();
                expenseAmountInput.readOnly = false;
                expenseAmountHint.classList.add('hidden');
                populateSubcategories();
                expenseModal.style.display = 'block';
                expenseAmountInput.focus();
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

            chartFilterButtons.addEventListener('click', (event) => {
                if (event.target.tagName !== 'BUTTON') return;
                const period = event.target.dataset.period;
                chartFilterButtons.querySelectorAll('button').forEach(btn => btn.classList.remove('active'));
                event.target.classList.add('active');
                currentChartPeriod = period;
                selectedChartCategoryId = null;
                selectedChartCategoryName = '';
                selectedChartMode = 'category';
                updateDashboard();
            });

            expensesFilterPresets.addEventListener('click', (event) => {
                if (event.target.tagName !== 'BUTTON') return;
                expensesPagePreset = event.target.dataset.preset;
                expensesFilterPresets.querySelectorAll('button').forEach(btn => btn.classList.remove('active'));
                event.target.classList.add('active');
                applyPresetDates(expensesPagePreset);
                filterExpensesPage();
            });

            applyExpensesFiltersBtn.addEventListener('click', filterExpensesPage);

            resetExpensesFiltersBtn.addEventListener('click', () => {
                expensesFilterSearch.value = '';
                expensesFilterCategory.value = '';
                expensesPagePreset = 'month';
                expensesFilterPresets.querySelectorAll('button').forEach(btn => btn.classList.toggle('active', btn.dataset.preset === 'month'));
                applyPresetDates('month');
                filterExpensesPage();
            });

            expensesPageLoadMoreBtn.addEventListener('click', () => {
                expensesVisibleCount += EXPENSES_PAGE_STEP;
                renderExpensesPage();
            });

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
                            subcategoryId: expenseSubcategorySelect.value || null,
                            description: expenseDescriptionInput.value,
                            date: expenseDateInput.value,
                        }),
                    });

                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({}));
                        expenseErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                        return;
                    }

                    expenseModal.style.display = 'none';
                    await updateDashboard();
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
                            date: incomeDateInput.value,
                        }),
                    });
                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({}));
                        incomeErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                        return;
                    }
                    incomeModal.style.display = 'none';
                    await updateDashboard();
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
                    if (response.status !== 201) {
                        const errorData = await response.json().catch(() => ({}));
                        categoryErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                        return;
                    }
                    categoryModal.style.display = 'none';
                    await fetchCategories();
                    await updateDashboard();
                } catch (error) {
                    categoryErrorDiv.textContent = `Ошибка: ${error.message}`;
                }
            });

            addSubcategoryBtn.addEventListener('click', async () => {
                expenseErrorDiv.textContent = '';
                try {
                    await createSubcategoryQuickly();
                } catch (error) {
                    expenseErrorDiv.textContent = `Ошибка: ${error.message}`;
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

            const handleExpenseListActions = async (event) => {
                const target = event.target.closest('button');
                if (!target) return;
                const id = target.dataset.id;
                const type = target.dataset.type;
                if (type !== 'expense') return;

                if (target.classList.contains('delete-btn')) {
                    if (!confirm('Вы уверены, что хотите удалить эту трату?')) return;
                    await apiFetch(`${API_URL}/expenses/${id}`, { method: 'DELETE' });
                    await updateDashboard();
                }

                if (target.classList.contains('edit-btn')) {
                    const response = await apiFetch(`${API_URL}/expenses/${id}`);
                    if (!response.ok) return;
                    const data = await response.json();
                    expenseModalTitle.textContent = 'Редактировать расход';
                    expenseIdInput.value = data.id;
                    expenseAmountInput.value = data.amount;
                    expenseDateInput.value = data.date;
                    expenseDescriptionInput.value = data.description;
                    expenseCategorySelect.value = data.category.id;
                    populateSubcategories(data.subcategory?.id);
                    const isReceiptExpense = !!data.receipt;
                    expenseAmountInput.readOnly = isReceiptExpense;
                    expenseAmountHint.classList.toggle('hidden', !isReceiptExpense);
                    expenseModal.style.display = 'block';
                    expenseDescriptionInput.focus();
                }
            };

            recentExpenseList.addEventListener('click', handleExpenseListActions);
            expensesPageList.addEventListener('click', handleExpenseListActions);

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

            expenseCategorySelect.addEventListener('change', () => {
                populateSubcategories();
            });

            window.addEventListener('online', () => {
                updateDashboard().catch(() => {});
            });

            renderPalettes();
            applyPresetDates(expensesPagePreset);
            chartFilterButtons.querySelectorAll('button').forEach(btn => btn.classList.toggle('active', btn.dataset.period === currentChartPeriod));
            expensesFilterPresets.querySelectorAll('button').forEach(btn => btn.classList.toggle('active', btn.dataset.preset === expensesPagePreset));

            await fetchCategories();
            await updateDashboard();
            setActiveView('dashboard-view');

            appInitialized = true;
            console.log('main.js: Инициализация завершена.');
        } catch (error) {
            console.error('Ошибка инициализации приложения:', error);
            appInitialized = false;
            const errorBlock = document.getElementById('dashboard-error-message');
            if (errorBlock) {
                errorBlock.textContent = 'Ошибка загрузки интерфейса. Обновите страницу.';
            }
        } finally {
            initInProgress = false;
        }
    }

    setupAuthPage();
    registerPwaSupport().catch(() => {});

    if (getToken()) {
        showApp();
        main().catch(err => console.error('Ошибка запуска:', err));
    } else {
        showAuth();
    }
})();
