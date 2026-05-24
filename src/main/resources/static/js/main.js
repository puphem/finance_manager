(function () {
    console.log('main.js: Скрипт начал выполняться.');

    const API_URL = '';

    const ICONS = ['fas fa-shopping-basket', 'fas fa-car', 'fas fa-taxi', 'fas fa-film', 'fas fa-file-invoice-dollar', 'fas fa-utensils', 'fas fa-house-user', 'fas fa-gas-pump', 'fas fa-plane', 'fas fa-heart', 'fas fa-gift', 'fas fa-tshirt', 'fas fa-paw', 'fas fa-graduation-cap', 'fas fa-spa', 'fas fa-heartbeat'];
    const COLORS = ['#27ae60', '#2980b9', '#1f8a70', '#f39c12', '#c0392b', '#8e44ad', '#2c3e50', '#16a085', '#d35400', '#7f8c8d', '#e74c3c', '#34495e', '#e84393'];

    const TOKEN_KEY = 'finance_jwt_token';
    const USERNAME_KEY = 'finance_username';
    const DISPLAY_NAME_KEY = 'finance_display_name';
    const THEME_KEY_PREFIX = 'finance_theme';
    const FONT_SIZE_KEY_PREFIX = 'finance_font_size';
    const RECURRING_RULES_KEY_PREFIX = 'finance_recurring_rules';
    const DEFAULT_EXPENSE_CATEGORY_NAME = 'продукты';
    const RECENT_EXPENSE_LIMIT = 7;
    const EXPENSES_PAGE_STEP = 20;
    const AUTO_SYNC_INTERVAL_MS = 30000;

    const getToken = () => localStorage.getItem(TOKEN_KEY);
    const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
    const getStoredUsername = () => localStorage.getItem(USERNAME_KEY);
    const setStoredUsername = (username) => localStorage.setItem(USERNAME_KEY, username);
    const getStoredDisplayName = () => localStorage.getItem(DISPLAY_NAME_KEY);
    const setStoredDisplayName = (displayName) => localStorage.setItem(DISPLAY_NAME_KEY, displayName);
    const getStorageScope = (username = getStoredUsername()) => {
        const normalizedUsername = (username || '').trim();
        if (normalizedUsername) return `user:${normalizedUsername}`;
        return 'anonymous';
    };
    const getScopedStorageKey = (prefix, username = getStoredUsername()) => {
        return `${prefix}:${getStorageScope(username)}`;
    };
    const logout = () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
        localStorage.removeItem(DISPLAY_NAME_KEY);
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
        const displayName = getStoredDisplayName() || getStoredUsername() || '';
        if (usernameEl) usernameEl.textContent = displayName;
    };

    const applyTheme = (theme) => {
        const nextTheme = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.dataset.theme = nextTheme;
        localStorage.setItem(getScopedStorageKey(THEME_KEY_PREFIX), nextTheme);
    };

    const applyFontSize = (size) => {
        const allowed = new Set(['small', 'normal', 'large']);
        const nextSize = allowed.has(size) ? size : 'normal';
        document.documentElement.dataset.fontSize = nextSize;
        localStorage.setItem(getScopedStorageKey(FONT_SIZE_KEY_PREFIX), nextSize);
    };

    const getFontScaleValue = () => {
        const raw = getComputedStyle(document.documentElement).getPropertyValue('--font-scale');
        const parsed = Number.parseFloat(raw);
        return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
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
        if (response.status === 401 || response.status === 403) {
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
                    setStoredDisplayName(data.displayName || data.username);
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
                    const data = await response.json().catch(() => null);
                    if (data?.token && data?.username) {
                        setToken(data.token);
                        setStoredUsername(data.username);
                        setStoredDisplayName(data.displayName || data.username);
                    } else {
                        const loginResponse = await fetch(`${API_URL}/auth/login`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ username, password }),
                        });
                        if (!loginResponse.ok) {
                            throw new Error('Регистрация успешна, но не удалось выполнить автологин.');
                        }
                        const loginData = await loginResponse.json();
                        setToken(loginData.token);
                        setStoredUsername(loginData.username);
                        setStoredDisplayName(loginData.displayName || loginData.username);
                    }
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
        const bottomNavBtns = Array.from(document.querySelectorAll('.bottom-nav-btn'));
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
            const toggleExpensesFiltersBtn = getElement('toggle-expenses-filters-btn');
            const expensesFiltersPanel = getElement('expenses-filters-panel');

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
            const receiptApiTokenInput = getElement('receipt-api-token-input');
            const saveReceiptApiTokenBtn = getElement('save-receipt-api-token-btn');
            const exportBackupBtn = getElement('export-backup-btn');
            const importBackupBtn = getElement('import-backup-btn');
            const importBackupFileInput = getElement('import-backup-file-input');
            const updateDisplayNameForm = getElement('update-display-name-form');
            const newUsernameInput = getElement('new-username-input');
            const displayNameInput = getElement('display-name-input');
            const updatePasswordForm = getElement('update-password-form');
            const passwordCurrentInput = getElement('password-current-input');
            const newPasswordInput = getElement('new-password-input');
            const newPasswordConfirmInput = getElement('new-password-confirm-input');
            const settingsAccountMessage = getElement('settings-account-message');

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
            const expenseRecurringEnabled = getElement('expense-recurring-enabled');
            const expenseRecurringOptions = getElement('expense-recurring-options');
            const expenseRecurringPeriod = getElement('expense-recurring-period');
            const expenseRecurringCustomDays = getElement('expense-recurring-custom-days');

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
            const incomeRecurringEnabled = getElement('income-recurring-enabled');
            const incomeRecurringOptions = getElement('income-recurring-options');
            const incomeRecurringPeriod = getElement('income-recurring-period');
            const incomeRecurringCustomDays = getElement('income-recurring-custom-days');

            const calendarDayChartModal = getElement('calendar-day-chart-modal');
            const dayChartDateLabel = getElement('day-chart-date-label');
            const dayChartPrevBtn = getElement('day-chart-prev-btn');
            const dayChartNextBtn = getElement('day-chart-next-btn');
            const dayExpenseChartCanvas = getElement('day-expense-chart');
            const dayChartEmpty = getElement('day-chart-empty');
            const dayExpensesFilterHint = getElement('day-expenses-filter-hint');
            const toggleDayExpensesBtn = getElement('toggle-day-expenses-btn');
            const dayExpensesList = getElement('day-expenses-list');

            const logoutBtn = getElement('logout-btn');
            logoutBtn.addEventListener('click', logout);

            const initialTheme = localStorage.getItem(getScopedStorageKey(THEME_KEY_PREFIX)) || 'light';
            const initialFontSize = localStorage.getItem(getScopedStorageKey(FONT_SIZE_KEY_PREFIX)) || 'normal';
            applyTheme(initialTheme);
            applyFontSize(initialFontSize);
            darkThemeToggle.checked = initialTheme === 'dark';
            fontSizeSelect.value = initialFontSize;
            receiptApiTokenInput.value = '';
            newUsernameInput.value = getStoredUsername() || '';
            displayNameInput.value = getStoredDisplayName() || getStoredUsername() || '';
            periodIncomeEl.classList.add('hidden');

            darkThemeToggle.addEventListener('change', () => {
                applyTheme(darkThemeToggle.checked ? 'dark' : 'light');
            });

            fontSizeSelect.addEventListener('change', () => {
                applyFontSize(fontSizeSelect.value);
                if (expenseChart) {
                    updateDashboard().catch(() => {});
                }
                if (dayChartDate) {
                    renderDayExpenseChart(dayChartDate);
                }
            });

            saveReceiptApiTokenBtn.addEventListener('click', () => {
                const token = (receiptApiTokenInput.value || '').trim();
                receiptApiTokenValue = token;
                showSettingsMessage(token
                    ? 'Токен для proverkachecka.com сохранен в текущей сессии.'
                    : 'Токен очищен. Используется серверный токен.');
            });

            toggleExpensesFiltersBtn.addEventListener('click', () => {
                const isHidden = expensesFiltersPanel.classList.toggle('hidden');
                toggleExpensesFiltersBtn.textContent = isHidden ? 'Показать фильтры' : 'Скрыть фильтры';
            });

            exportBackupBtn.addEventListener('click', async () => {
                try {
                    const response = await apiFetch(`${API_URL}/backup/export`);
                    if (!response.ok) {
                        const err = await response.json().catch(() => ({}));
                        throw new Error(err.message || 'Не удалось экспортировать backup');
                    }
                    const data = await response.json();
                    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
                    const link = document.createElement('a');
                    link.href = URL.createObjectURL(blob);
                    link.download = `finance-backup-${getTodayDateString()}.json`;
                    document.body.appendChild(link);
                    link.click();
                    URL.revokeObjectURL(link.href);
                    link.remove();
                    showSettingsMessage('Backup успешно экспортирован.');
                } catch (error) {
                    showSettingsMessage(error.message || 'Ошибка экспорта backup', true);
                }
            });

            importBackupBtn.addEventListener('click', () => importBackupFileInput.click());
            importBackupFileInput.addEventListener('change', async () => {
                const file = importBackupFileInput.files?.[0];
                if (!file) return;
                try {
                    const text = await file.text();
                    const payload = JSON.parse(text);
                    const response = await apiFetch(`${API_URL}/backup/import`, {
                        method: 'POST',
                        body: JSON.stringify(payload),
                    });
                    if (!response.ok) {
                        const err = await response.json().catch(() => ({}));
                        throw new Error(err.message || 'Не удалось импортировать backup');
                    }
                    await fetchCategories();
                    await updateDashboard();
                    showSettingsMessage('Backup успешно импортирован.');
                } catch (error) {
                    showSettingsMessage(error.message || 'Ошибка импорта backup', true);
                } finally {
                    importBackupFileInput.value = '';
                }
            });

            const showSettingsMessage = (message, isError = false) => {
                settingsAccountMessage.textContent = message;
                settingsAccountMessage.style.color = isError ? 'var(--danger)' : 'var(--success)';
                setTimeout(() => {
                    if (settingsAccountMessage.textContent === message) {
                        settingsAccountMessage.textContent = '';
                        settingsAccountMessage.style.color = '';
                    }
                }, 4000);
            };

            updateDisplayNameForm.addEventListener('submit', async (event) => {
                event.preventDefault();
                const displayName = (displayNameInput.value || '').trim();
                if (!displayName) {
                    showSettingsMessage('Введите имя пользователя.', true);
                    return;
                }
                try {
                    const response = await apiFetch(`${API_URL}/account/display-name`, {
                        method: 'PUT',
                        body: JSON.stringify({ displayName }),
                    });
                    if (!response.ok) {
                        const err = await response.json().catch(() => ({}));
                        showSettingsMessage(err.message || 'Не удалось обновить имя пользователя.', true);
                        return;
                    }
                    const data = await response.json();
                    setToken(data.token);
                    setStoredDisplayName(data.displayName || data.username);
                    showApp();
                    displayNameInput.value = data.displayName || data.username || displayName;
                    showSettingsMessage('Имя пользователя обновлено.');
                    await updateDashboard();
                } catch (error) {
                    showSettingsMessage(error.message || 'Ошибка обновления имени пользователя.', true);
                }
            });

            updatePasswordForm.addEventListener('submit', async (event) => {
                event.preventDefault();
                const currentPassword = (passwordCurrentInput.value || '').trim();
                const newPassword = (newPasswordInput.value || '').trim();
                const confirmPassword = (newPasswordConfirmInput.value || '').trim();
                if (!currentPassword || !newPassword || !confirmPassword) {
                    showSettingsMessage('Заполните все поля для смены пароля.', true);
                    return;
                }
                if (newPassword !== confirmPassword) {
                    showSettingsMessage('Новый пароль и подтверждение не совпадают.', true);
                    return;
                }
                try {
                    const response = await apiFetch(`${API_URL}/account/password`, {
                        method: 'PUT',
                        body: JSON.stringify({ currentPassword, newPassword }),
                    });
                    if (!response.ok) {
                        const err = await response.json().catch(() => ({}));
                        showSettingsMessage(err.message || 'Не удалось обновить пароль.', true);
                        return;
                    }
                    const data = await response.json();
                    setToken(data.token);
                    passwordCurrentInput.value = '';
                    newPasswordInput.value = '';
                    newPasswordConfirmInput.value = '';
                    showSettingsMessage('Пароль успешно обновлен.');
                } catch (error) {
                    showSettingsMessage(error.message || 'Ошибка обновления пароля.', true);
                }
            });

            const setActiveView = (viewId) => {
                currentViewId = viewId;
                appViews.forEach(view => {
                    view.classList.toggle('hidden', view.id !== viewId);
                });
                navButtons.forEach(btn => {
                    btn.classList.toggle('active', btn.dataset.view === viewId);
                });
                bottomNavBtns.forEach(btn => {
                    btn.classList.toggle('active', btn.dataset.view === viewId);
                });
                if (viewId === 'calendar-view') renderCalendarView();
            };

            navButtons.forEach(btn => {
                btn.addEventListener('click', () => setActiveView(btn.dataset.view));
            });

            bottomNavBtns.forEach(btn => {
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
            let allTransactionsCache = [];
            let filteredExpensesPage = [];
            let expensesVisibleCount = EXPENSES_PAGE_STEP;
            let expenseChart = null;
            let expensesPagePreset = 'month';
            let calendarYear = new Date().getFullYear();
            let calendarMonth = new Date().getMonth();
            let calendarExpensesCache = null;
            let qaType = 'expense';
            let dayChartDate = null;
            let dayExpenseChart = null;
            let dayChartMode = 'category';
            let dayChartSelectedCategoryId = null;
            let dayChartSelectedCategoryName = '';
            let receiptApiTokenValue = '';
            let currentViewId = 'dashboard-view';
            let autoSyncTimerId = null;
            let autoSyncInProgress = false;

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

            const selectDefaultExpenseCategory = () => {
                const categories = Array.from(categoryById.values());
                if (categories.length === 0) return;
                const preferred = categories.find(category => {
                    const normalized = (category.name || '').trim().toLowerCase();
                    return normalized === DEFAULT_EXPENSE_CATEGORY_NAME || normalized.startsWith(`${DEFAULT_EXPENSE_CATEGORY_NAME} `);
                });
                const fallback = preferred || categories[0];
                expenseCategorySelect.value = String(fallback.id);
                populateSubcategories();
            };

            const populateExpenseCategoryFilter = () => {
                const currentValue = expensesFilterCategory.value;
                expensesFilterCategory.innerHTML = '';
                const allOption = document.createElement('option');
                allOption.value = '';
                allOption.textContent = 'Все категории';
                expensesFilterCategory.appendChild(allOption);
                const incomeOption = document.createElement('option');
                incomeOption.value = '__income__';
                incomeOption.textContent = 'Доходы';
                expensesFilterCategory.appendChild(incomeOption);

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
                if (currentCategoryValue && categoryById.has(Number(currentCategoryValue))) {
                    expenseCategorySelect.value = currentCategoryValue;
                    populateSubcategories();
                } else {
                    selectDefaultExpenseCategory();
                }
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
                } else {
                    expensesFilterStart.value = '';
                    expensesFilterEnd.value = '';
                    return;
                }

                expensesFilterStart.value = toDateInputValue(start);
                expensesFilterEnd.value = toDateInputValue(end);
            };

            const getSubcategoryTint = (categoryColor, index = 0) => {
                // Shift pattern intentionally alternates strong dark and light offsets,
                // then medium offsets, so adjacent generated colors differ more clearly.
                const shifts = [0, -72, 72, -44, 44, -22, 22, -86, 86, -58, 58];
                const shift = shifts[index % shifts.length];
                return adjustHexColor(categoryColor || '#95a5a6', shift);
            };

            const formatDateHeaderRu = (isoDate) => {
                const date = new Date(`${isoDate}T00:00:00`);
                return date.toLocaleDateString('ru-RU', { day: '2-digit', month: 'long', year: 'numeric' });
            };

            const getChartFontSizes = () => {
                const scale = getFontScaleValue();
                return {
                    legend: Math.max(11, Math.round(12 * scale)),
                    title: Math.max(12, Math.round(14 * scale)),
                    center: Math.max(12, Math.round(14 * scale)),
                };
            };

            const findOtherCategory = () => {
                for (const cat of categoryById.values()) {
                    const name = (cat.name || '').trim().toLowerCase();
                    if (name === 'прочее') return cat;
                }
                return null;
            };

            const getRecurringIntervalDays = (periodValue, customValue) => {
                if (periodValue === 'custom') {
                    const parsed = Number(customValue);
                    return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : null;
                }
                const parsed = Number(periodValue);
                return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
            };

            const getStoredRecurringRules = () => {
                try {
                    const parsed = JSON.parse(localStorage.getItem(getScopedStorageKey(RECURRING_RULES_KEY_PREFIX)) || '[]');
                    return Array.isArray(parsed) ? parsed : [];
                } catch (_) {
                    return [];
                }
            };

            const saveRecurringRules = (rules) => {
                localStorage.setItem(getScopedStorageKey(RECURRING_RULES_KEY_PREFIX), JSON.stringify(rules));
            };

            const addRecurringRule = (rule) => {
                const rules = getStoredRecurringRules();
                rules.push(rule);
                saveRecurringRules(rules);
            };

            const processRecurringRules = async () => {
                const rules = getStoredRecurringRules();
                if (rules.length === 0) return;
                const today = new Date();
                const todayIso = toDateInputValue(today);
                let changed = false;

                for (const rule of rules) {
                    const intervalDays = Number(rule.intervalDays || 0);
                    if (!intervalDays || intervalDays < 1) continue;
                    let cursor = new Date(`${rule.lastRunDate}T00:00:00`);
                    if (Number.isNaN(cursor.getTime())) continue;

                    while (true) {
                        cursor.setDate(cursor.getDate() + intervalDays);
                        const nextIso = toDateInputValue(cursor);
                        if (nextIso > todayIso) break;

                        const endpoint = rule.type === 'income' ? `${API_URL}/incomes` : `${API_URL}/expenses`;
                        const payload = { ...rule.payload, date: nextIso };
                        const response = await apiFetch(endpoint, {
                            method: 'POST',
                            body: JSON.stringify(payload),
                        });
                        if (!response.ok) break;
                        rule.lastRunDate = nextIso;
                        changed = true;
                    }
                }

                saveRecurringRules(rules);
                if (changed) await refreshAllExpenses();
            };

            const renderExpenseRow = (expense, index) => {
                const item = document.createElement('li');
                item.classList.add('swipeable');
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
                    <div class="swipe-action-bg swipe-action-right">✏️</div>
                    <div class="swipe-action-bg swipe-action-left">🗑️</div>
                    <div style="font-size:1.35em; color:${category.color || '#7f8c8d'};"><i class="${icon}"></i></div>
                    <div class="transaction-main">
                        <div class="transaction-title">
                            <span><strong>${amountText}</strong> — ${safeDescription}</span>
                            ${subcategoryBadge}
                        </div>
                        <small class="transaction-date">${date}</small>
                    </div>
                    <div class="transaction-actions">
                        <button class="repeat-btn" data-id="${expense.id}" data-type="expense" title="Повторить">🔄</button>
                        <button class="edit-btn" data-id="${expense.id}" data-type="expense">✏️</button>
                        <button class="delete-btn" data-id="${expense.id}" data-type="expense">🗑️</button>
                    </div>`;
                attachSwipeHandlers(item, expense.id, 'expense');
                return item;
            };

            const renderIncomeRow = (income) => {
                const item = document.createElement('li');
                item.classList.add('swipeable');
                const date = new Date(income.date).toLocaleDateString('ru-RU');
                const amountText = `+${Number(income.amount || 0).toFixed(2)} руб.`;
                const safeDescription = escapeHtml(income.description || 'Доход');

                item.innerHTML = `
                    <div class="swipe-action-bg swipe-action-right">✏️</div>
                    <div class="swipe-action-bg swipe-action-left">🗑️</div>
                    <div style="font-size:1.35em; color:var(--success);"><i class="fas fa-arrow-trend-up"></i></div>
                    <div class="transaction-main">
                        <div class="transaction-title">
                            <span><strong style="color: var(--success);">${amountText}</strong> — ${safeDescription}</span>
                        </div>
                        <small class="transaction-date">${date}</small>
                    </div>
                    <div class="transaction-actions">
                        <button class="repeat-btn" data-id="${income.id}" data-type="income" title="Повторить">🔄</button>
                        <button class="edit-btn" data-id="${income.id}" data-type="income">✏️</button>
                        <button class="delete-btn" data-id="${income.id}" data-type="income">🗑️</button>
                    </div>`;
                attachSwipeHandlers(item, income.id, 'income');
                return item;
            };

            const renderTransactionRow = (transaction, index) => {
                if (transaction.entryType === 'income') return renderIncomeRow(transaction);
                return renderExpenseRow(transaction, index);
            };

            const getApiPathByType = (type) => type === 'income' ? 'incomes' : 'expenses';

            const openIncomeEditForm = (data) => {
                incomeModalTitle.textContent = 'Редактировать доход';
                incomeIdInput.value = data.id;
                incomeAmountInput.value = data.amount;
                incomeDateInput.value = data.date;
                incomeDescriptionInput.value = data.description || '';
                incomeRecurringEnabled.checked = false;
                incomeRecurringPeriod.value = '1';
                incomeRecurringCustomDays.value = '';
                updateRecurringUiState('income');
                incomeErrorDiv.textContent = '';
                incomeModal.style.display = 'block';
                incomeDescriptionInput.focus();
            };

            /* ======= SWIPE ACTIONS ======= */
            const attachSwipeHandlers = (item, entryId, entryType) => {
                let startX = 0;
                let currentX = 0;
                let swiping = false;
                const THRESHOLD = 100;

                const reset = () => {
                    item.style.transform = '';
                    item.querySelectorAll('.swipe-action-bg').forEach(el => { el.style.opacity = '0'; });
                    swiping = false;
                };

                item.addEventListener('touchstart', (e) => {
                    startX = e.touches[0].clientX;
                    currentX = startX;
                    swiping = true;
                }, { passive: true });

                item.addEventListener('touchmove', (e) => {
                    if (!swiping) return;
                    currentX = e.touches[0].clientX;
                    const dx = currentX - startX;
                    const clamped = Math.max(-110, Math.min(110, dx));
                    item.style.transform = `translateX(${clamped}px)`;
                    const leftBg = item.querySelector('.swipe-action-right');
                    const rightBg = item.querySelector('.swipe-action-left');
                    if (dx > 0 && leftBg) leftBg.style.opacity = Math.min(1, dx / THRESHOLD).toFixed(2);
                    if (dx < 0 && rightBg) rightBg.style.opacity = Math.min(1, -dx / THRESHOLD).toFixed(2);
                }, { passive: true });

                item.addEventListener('touchend', async () => {
                    if (!swiping) return;
                    const dx = currentX - startX;
                    reset();
                    if (dx > THRESHOLD) {
                        // swipe right → edit
                        const response = await apiFetch(`${API_URL}/${getApiPathByType(entryType)}/${entryId}`);
                        if (!response.ok) return;
                        const data = await response.json();
                        if (entryType === 'income') openIncomeEditForm(data);
                        else openExpenseEditForm(data);
                    } else if (dx < -THRESHOLD) {
                        // swipe left → delete
                        if (confirm(entryType === 'income' ? 'Удалить доход?' : 'Удалить запись о расходе?')) {
                            await apiFetch(`${API_URL}/${getApiPathByType(entryType)}/${entryId}`, { method: 'DELETE' });
                            await updateDashboard();
                        }
                    }
                });
            };

            const openExpenseEditForm = (data) => {
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
                expenseRecurringEnabled.checked = false;
                expenseRecurringPeriod.value = '1';
                expenseRecurringCustomDays.value = '';
                updateRecurringUiState('expense');
                expenseErrorDiv.textContent = '';
                expenseModal.style.display = 'block';
                expenseDescriptionInput.focus();
            };

            /* ======= CALENDAR VIEW ======= */
            const MONTH_NAMES_RU = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];

            const hexToRgba = (hex, alpha) => {
                const rgb = parseHexColor(hex);
                if (!rgb) return `rgba(149,165,166,${alpha})`;
                return `rgba(${rgb.r},${rgb.g},${rgb.b},${alpha})`;
            };

            const renderCalendarView = async () => {
                const monthLabel = document.getElementById('calendar-month-label');
                const grid = document.getElementById('calendar-grid');
                const legend = document.getElementById('calendar-legend');
                if (!grid) return;
                monthLabel.textContent = `${MONTH_NAMES_RU[calendarMonth]} ${calendarYear}`;

                // Determine first/last day of month
                const firstDay = new Date(calendarYear, calendarMonth, 1);
                const lastDay = new Date(calendarYear, calendarMonth + 1, 0);
                const startStr = toDateInputValue(firstDay);
                const endStr = toDateInputValue(lastDay);

                // Use already loaded cache and fallback to fetch if cache is not ready yet
                let sourceExpenses = Array.isArray(allExpensesCache) ? allExpensesCache : [];
                if (sourceExpenses.length === 0) {
                    try {
                        const response = await apiFetch(`${API_URL}/expenses?period=all`);
                        if (!response.ok) return;
                        sourceExpenses = await response.json();
                    } catch (e) {
                        console.error('Calendar fetch error', e);
                        return;
                    }
                }
                calendarExpensesCache = sourceExpenses.filter(exp => {
                    const d = exp.date ? exp.date.slice(0, 10) : '';
                    return d >= startStr && d <= endStr;
                });

                // Aggregate by day
                const dayMap = {}; // day -> { total, categoryTotals: { id: { total, color, name } } }
                calendarExpensesCache.forEach(exp => {
                    const day = parseInt((exp.date || '').slice(8, 10), 10);
                    if (!day) return;
                    if (!dayMap[day]) dayMap[day] = { total: 0, categoryTotals: {} };
                    const amount = Number(exp.amount || 0);
                    dayMap[day].total += amount;
                    const catId = exp.category?.id;
                    if (catId) {
                        if (!dayMap[day].categoryTotals[catId]) {
                            dayMap[day].categoryTotals[catId] = { total: 0, color: exp.category.color || '#95a5a6', name: exp.category.name || '' };
                        }
                        dayMap[day].categoryTotals[catId].total += amount;
                    }
                });

                // Max day total for heatmap
                const totals = Object.values(dayMap).map(d => d.total);
                const maxTotal = totals.length > 0 ? Math.max(...totals) : 1;

                // Day-of-week offset for first day (Monday = 0)
                let startDow = firstDay.getDay(); // 0=Sun,1=Mon,...
                startDow = startDow === 0 ? 6 : startDow - 1; // convert to Mon=0

                const today = new Date();
                const todayStr = toDateInputValue(today);

                grid.innerHTML = '';

                // Empty cells before first day
                for (let i = 0; i < startDow; i++) {
                    const empty = document.createElement('div');
                    empty.className = 'calendar-day empty';
                    grid.appendChild(empty);
                }

                for (let d = 1; d <= lastDay.getDate(); d++) {
                    const cell = document.createElement('div');
                    cell.className = 'calendar-day';
                    const dateStr = `${calendarYear}-${String(calendarMonth + 1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
                    if (dateStr === todayStr) cell.classList.add('today');

                    const dayData = dayMap[d];
                    if (dayData && dayData.total > 0) {
                        // Dominant category
                        const domCat = Object.values(dayData.categoryTotals).reduce((a, b) => a.total >= b.total ? a : b, { total: 0, color: '#95a5a6' });
                        const intensity = Math.min(0.85, (dayData.total / maxTotal) * 0.85);
                        cell.style.background = hexToRgba(domCat.color, intensity);
                        const textLight = intensity > 0.45;
                        cell.style.color = textLight ? '#fff' : 'var(--text-color)';

                        const numEl = document.createElement('div');
                        numEl.className = 'calendar-day-num';
                        numEl.textContent = d;
                        const sumEl = document.createElement('div');
                        sumEl.className = 'calendar-day-sum';
                        sumEl.textContent = dayData.total >= 1000
                            ? `${(dayData.total / 1000).toFixed(1)}к`
                            : dayData.total.toFixed(0);
                        cell.appendChild(numEl);
                        cell.appendChild(sumEl);
                    } else {
                        cell.style.background = 'var(--surface-alt)';
                        const numEl = document.createElement('div');
                        numEl.className = 'calendar-day-num';
                        numEl.textContent = d;
                        cell.appendChild(numEl);
                    }
                    cell.dataset.date = dateStr;
                    cell.addEventListener('click', () => openDayChart(dateStr));
                    grid.appendChild(cell);
                }

                // Legend: unique categories in this month
                const usedCats = {};
                Object.values(dayMap).forEach(dd => {
                    Object.values(dd.categoryTotals).forEach(ct => {
                        usedCats[ct.name] = ct.color;
                    });
                });
                legend.innerHTML = '';
                Object.entries(usedCats).forEach(([name, color]) => {
                    const item = document.createElement('div');
                    item.className = 'calendar-legend-item';
                    item.innerHTML = `<div class="calendar-legend-dot" style="background:${color}"></div><span>${escapeHtml(name)}</span>`;
                    legend.appendChild(item);
                });
            };

            const renderDayExpenseChart = (targetDateIso) => {
                dayChartDate = targetDateIso;
                const targetDate = new Date(`${targetDateIso}T00:00:00`);
                dayChartDateLabel.textContent = targetDate.toLocaleDateString('ru-RU', {
                    day: '2-digit',
                    month: 'long',
                    year: 'numeric',
                });

                const dayExpenses = allExpensesCache.filter(exp => (exp.date || '').slice(0, 10) === targetDateIso);
                const chartFont = getChartFontSizes();
                const byCategory = new Map();
                dayExpenses.forEach(exp => {
                    const categoryKey = Number(exp.category?.id || 0);
                    const amount = Number(exp.amount || 0);
                    if (!byCategory.has(categoryKey)) {
                        byCategory.set(categoryKey, {
                            categoryId: categoryKey,
                            label: exp.category?.name || 'Без категории',
                            color: exp.category?.color || '#95a5a6',
                            total: 0,
                            bySubcategory: new Map(),
                        });
                    }
                    const categoryBucket = byCategory.get(categoryKey);
                    categoryBucket.total += amount;

                    const subcategoryKey = Number(exp.subcategory?.id || 0);
                    if (!categoryBucket.bySubcategory.has(subcategoryKey)) {
                        categoryBucket.bySubcategory.set(subcategoryKey, {
                            subcategoryId: subcategoryKey,
                            label: exp.subcategory?.name || 'Без подкатегории',
                            total: 0,
                        });
                    }
                    categoryBucket.bySubcategory.get(subcategoryKey).total += amount;
                });

                const categorySummary = Array.from(byCategory.values()).sort((a, b) => b.total - a.total);
                let summary = categorySummary;
                let title = 'Расходы за день';
                let filteredDayExpenses = dayExpenses;
                if (dayChartMode === 'subcategory' && dayChartSelectedCategoryId !== null) {
                    const selectedCategory = categorySummary.find(item => item.categoryId === dayChartSelectedCategoryId);
                    if (selectedCategory) {
                        summary = Array.from(selectedCategory.bySubcategory.values())
                            .map((sub, index) => ({ ...sub, color: getSubcategoryTint(selectedCategory.color, index) }))
                            .sort((a, b) => b.total - a.total);
                        title = `Подкатегории: ${selectedCategory.label}`;
                        dayChartSelectedCategoryName = selectedCategory.label;
                        filteredDayExpenses = dayExpenses.filter(exp => Number(exp.category?.id || 0) === dayChartSelectedCategoryId);
                    } else {
                        dayChartMode = 'category';
                        dayChartSelectedCategoryId = null;
                        dayChartSelectedCategoryName = '';
                    }
                }

                if (dayExpenseChart) dayExpenseChart.destroy();
                dayExpensesList.innerHTML = '';
                filteredDayExpenses.forEach(expense => {
                    const item = document.createElement('li');
                    item.innerHTML = `
                        <div class="transaction-main">
                            <div class="transaction-title">
                                <strong>${escapeHtml(expense.description || 'Без описания')}</strong>
                                <span>${escapeHtml(expense.category?.name || 'Без категории')}${expense.subcategory?.name ? ` • ${escapeHtml(expense.subcategory.name)}` : ''}</span>
                            </div>
                            <strong>-${Number(expense.amount || 0).toFixed(2)} руб.</strong>
                        </div>
                    `;
                    dayExpensesList.appendChild(item);
                });
                if (dayChartMode === 'subcategory' && dayChartSelectedCategoryName) {
                    dayExpensesFilterHint.textContent = `Показаны траты категории: ${dayChartSelectedCategoryName}`;
                    dayExpensesFilterHint.classList.remove('hidden');
                } else {
                    dayExpensesFilterHint.textContent = '';
                    dayExpensesFilterHint.classList.add('hidden');
                }

                if (summary.length === 0) {
                    dayChartEmpty.classList.remove('hidden');
                    dayExpenseChartCanvas.classList.add('hidden');
                    return;
                }

                dayChartEmpty.classList.add('hidden');
                dayExpenseChartCanvas.classList.remove('hidden');
                dayExpenseChart = new Chart(dayExpenseChartCanvas, {
                    type: 'doughnut',
                    data: {
                        labels: summary.map(item => item.label),
                        datasets: [{
                            data: summary.map(item => Number(item.total.toFixed(2))),
                            backgroundColor: summary.map(item => item.color),
                            borderColor: '#fff',
                            borderWidth: 2,
                        }],
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: { position: 'top', labels: { font: { size: chartFont.legend } } },
                            title: { display: true, text: title, font: { size: chartFont.title } },
                        },
                        onClick: (event, elements, chart) => {
                            if (dayChartMode === 'subcategory') {
                                const firstArc = chart.getDatasetMeta(0)?.data?.[0];
                                if (firstArc) {
                                    const clickX = Number(event?.native?.offsetX ?? event?.x);
                                    const clickY = Number(event?.native?.offsetY ?? event?.y);
                                    if (!Number.isFinite(clickX) || !Number.isFinite(clickY)) {
                                        return;
                                    }
                                    const dx = clickX - firstArc.x;
                                    const dy = clickY - firstArc.y;
                                    const distance = Math.sqrt(dx * dx + dy * dy);
                                    if (distance <= firstArc.innerRadius) {
                                        dayChartSelectedCategoryId = null;
                                        dayChartSelectedCategoryName = '';
                                        dayChartMode = 'category';
                                        renderDayExpenseChart(targetDateIso);
                                        return;
                                    }
                                }
                            }
                            if (dayChartMode === 'category' && elements.length > 0) {
                                const nextCategory = summary[elements[0].index];
                                dayChartSelectedCategoryId = nextCategory?.categoryId ?? null;
                                dayChartSelectedCategoryName = nextCategory?.label || '';
                                dayChartMode = 'subcategory';
                                renderDayExpenseChart(targetDateIso);
                            }
                        },
                    },
                    plugins: [{
                        id: 'dayChartCenterReturnHint',
                        afterDraw(chart) {
                            if (dayChartMode !== 'subcategory') return;
                            const { ctx, chartArea: { left, right, top, bottom } } = chart;
                            const centerX = (left + right) / 2;
                            const centerY = (top + bottom) / 2;
                            ctx.save();
                            ctx.font = `600 ${chartFont.center}px Arial`;
                            ctx.fillStyle = '#666';
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillText('Назад', centerX, centerY);
                            ctx.restore();
                        }
                    }]
                });
            };

            const openDayChart = (targetDateIso) => {
                dayChartMode = 'category';
                dayChartSelectedCategoryId = null;
                dayChartSelectedCategoryName = '';
                dayExpensesFilterHint.textContent = '';
                dayExpensesFilterHint.classList.add('hidden');
                dayExpensesList.classList.add('hidden');
                toggleDayExpensesBtn.textContent = 'Показать траты за день';
                renderDayExpenseChart(targetDateIso);
                calendarDayChartModal.style.display = 'block';
            };

            const shiftDayChartDate = (deltaDays) => {
                if (!dayChartDate) return;
                const date = new Date(`${dayChartDate}T00:00:00`);
                date.setDate(date.getDate() + deltaDays);
                renderDayExpenseChart(toDateInputValue(date));
            };

            dayChartPrevBtn.addEventListener('click', () => shiftDayChartDate(-1));
            dayChartNextBtn.addEventListener('click', () => shiftDayChartDate(1));
            toggleDayExpensesBtn.addEventListener('click', () => {
                const isHidden = dayExpensesList.classList.toggle('hidden');
                toggleDayExpensesBtn.textContent = isHidden ? 'Показать траты за день' : 'Скрыть траты за день';
            });

            document.getElementById('calendar-prev-btn')?.addEventListener('click', () => {
                calendarMonth--;
                if (calendarMonth < 0) { calendarMonth = 11; calendarYear--; }
                renderCalendarView();
            });
            document.getElementById('calendar-next-btn')?.addEventListener('click', () => {
                calendarMonth++;
                if (calendarMonth > 11) { calendarMonth = 0; calendarYear++; }
                renderCalendarView();
            });

            /* ======= QUICK ADD (умное распознавание) ======= */
            const INCOME_KEYWORDS = ['зарплата','зп','аванс','бонус','премия','перевод','вернул','долг','выручка','продал','подарок','мама','папа','родители','брат','сестра','друг','получил'];
            const CATEGORY_KEYWORDS = {
                'продукты': ['молоко','хлеб','мясо','рыба','овощи','фрукты','яйца','масло','крупа','сыр','колбаса','напиток','сок','вода','кефир','творог','магнит','пятерочка','перекресток','лента','ашан','дикси','продукты'],
                'кафе': ['кофе','чай','капучино','латте','кофейня','кафе','ресторан','бар','пицца','суши','бургер','фастфуд','шаурма','обед','ужин','завтрак','еда на вынос'],
                'транспорт': ['автобус','трамвай','троллейбус','метро','электричка','поезд','самолет','билет','проезд'],
                'такси': ['такси','убер','яндекс такси','яндекс.такси','lyft','bolt'],
                'здоровье': ['аптека','лекарство','таблетки','врач','больница','клиника','медицина','витамин'],
                'развлечения': ['кино','театр','концерт','игра','парк','аттракцион','выставка','музей','спорт'],
                'подписки': ['подписка','netflix','spotify','youtube','яндекс','vk','telegram','apple','google','icloud','стриминг'],
                'одежда': ['одежда','обувь','куртка','брюки','джинсы','рубашка','футболка','магазин'],
                'дом': ['квартплата','коммунальные','электричество','газ','интернет','телефон','ремонт','мебель','посуда'],
                'питомцы': ['корм','ветеринар','питомец','кошка','собака','зоомагазин'],
            };

            const classifyEntry = (text) => {
                const lower = text.trim().toLowerCase();
                if (!lower) return { type: qaType, categoryKeyword: null };

                // check income
                const isIncome = INCOME_KEYWORDS.some(kw => lower.includes(kw));
                if (isIncome) return { type: 'income', categoryKeyword: null };

                // check expense category
                for (const [catKw, keywords] of Object.entries(CATEGORY_KEYWORDS)) {
                    if (keywords.some(kw => lower.includes(kw)) || lower.includes(catKw)) {
                        return { type: 'expense', categoryKeyword: catKw };
                    }
                }
                return { type: 'expense', categoryKeyword: null };
            };

            const findCategoryByKeyword = (keyword) => {
                if (!keyword) return null;
                for (const cat of categoryById.values()) {
                    if ((cat.name || '').toLowerCase().includes(keyword)) return cat;
                }
                return null;
            };

            const quickAddModal = document.getElementById('quick-add-modal');
            const quickAddForm = document.getElementById('quick-add-form');
            const qaDescInput = document.getElementById('qa-description');
            const qaAmountInput = document.getElementById('qa-amount');
            const qaDetectedHint = document.getElementById('qa-detected-hint');
            const qaErrorDiv = document.getElementById('quick-add-error');
            const qaTypeBtns = { expense: document.getElementById('qa-type-expense'), income: document.getElementById('qa-type-income') };

            const setQaType = (type) => {
                qaType = type;
                qaTypeBtns.expense.classList.toggle('active', type === 'expense');
                qaTypeBtns.income.classList.toggle('active', type === 'income');
            };

            qaTypeBtns.expense?.addEventListener('click', () => setQaType('expense'));
            qaTypeBtns.income?.addEventListener('click', () => setQaType('income'));

            const openQuickAdd = () => {
                qaType = 'expense';
                setQaType('expense');
                quickAddForm.reset();
                qaDetectedHint.textContent = '';
                qaErrorDiv.textContent = '';
                quickAddModal.style.display = 'block';
                qaDescInput.focus();
            };

            document.getElementById('bottom-nav-plus')?.addEventListener('click', openQuickAdd);

            qaDescInput?.addEventListener('input', () => {
                const text = qaDescInput.value;
                if (!text.trim()) { qaDetectedHint.textContent = ''; return; }
                const result = classifyEntry(text);
                setQaType(result.type);
                const cat = result.categoryKeyword ? findCategoryByKeyword(result.categoryKeyword) : null;
                const fallbackCat = findOtherCategory();
                if (result.type === 'income') {
                    qaDetectedHint.textContent = '💰 Определено как доход';
                } else if (cat) {
                    qaDetectedHint.textContent = `🛒 Расход → категория «${cat.name}»`;
                } else if (fallbackCat) {
                    qaDetectedHint.textContent = `🧾 Расход → категория «${fallbackCat.name}»`;
                } else {
                    qaDetectedHint.textContent = '🧾 Расход (категория не определена)';
                }
            });

            quickAddForm?.addEventListener('submit', async (e) => {
                e.preventDefault();
                qaErrorDiv.textContent = '';
                const text = (qaDescInput.value || '').trim();
                const amount = qaAmountInput.value;
                const result = classifyEntry(text);
                const today = getTodayDateString();
                try {
                    if (result.type === 'income') {
                        const res = await apiFetch(`${API_URL}/incomes`, {
                            method: 'POST',
                            body: JSON.stringify({ amount, description: text || 'Доход', date: today }),
                        });
                        if (!res.ok) {
                            const err = await res.json().catch(() => ({}));
                            qaErrorDiv.textContent = err.message || 'Ошибка сохранения';
                            return;
                        }
                    } else {
                        const cat = result.categoryKeyword ? findCategoryByKeyword(result.categoryKeyword) : null;
                        const fallbackCat = findOtherCategory();
                        const firstCat = categoryById.values().next().value;
                        const categoryId = cat?.id || fallbackCat?.id || firstCat?.id;
                        if (!categoryId) { qaErrorDiv.textContent = 'Нет категорий. Создайте хотя бы одну.'; return; }
                        const res = await apiFetch(`${API_URL}/expenses`, {
                            method: 'POST',
                            body: JSON.stringify({ amount, categoryId, description: text || '', date: today }),
                        });
                        if (!res.ok) {
                            const err = await res.json().catch(() => ({}));
                            qaErrorDiv.textContent = err.message || 'Ошибка сохранения';
                            return;
                        }
                    }
                    quickAddModal.style.display = 'none';
                    await updateDashboard();
                } catch (err) {
                    qaErrorDiv.textContent = err.message || 'Ошибка';
                }
            });

            const renderRecentExpenses = () => {
                recentExpenseList.innerHTML = '';
                const recent = allTransactionsCache.slice(0, RECENT_EXPENSE_LIMIT);
                if (recent.length === 0) {
                    recentExpenseList.innerHTML = '<li>Пока нет записей.</li>';
                    return;
                }
                recent.forEach((entry, index) => recentExpenseList.appendChild(renderTransactionRow(entry, index)));
            };

            const renderExpensesPage = () => {
                expensesPageList.innerHTML = '';
                const visible = filteredExpensesPage.slice(0, expensesVisibleCount);
                let lastDate = '';
                visible.forEach((expense, index) => {
                    const entryDate = (expense.date || '').slice(0, 10);
                    if (entryDate && entryDate !== lastDate) {
                        const separator = document.createElement('li');
                        separator.className = 'expense-date-separator';
                        separator.innerHTML = `<span>${formatDateHeaderRu(entryDate)}</span>`;
                        expensesPageList.appendChild(separator);
                        lastDate = entryDate;
                    }
                    expensesPageList.appendChild(renderTransactionRow(expense, index));
                });

                expensesPageSummary.textContent = `Найдено: ${filteredExpensesPage.length}`;
                expensesPageLoadMoreBtn.classList.toggle('hidden', filteredExpensesPage.length <= expensesVisibleCount);
            };

            const filterExpensesPage = () => {
                const selectedCategory = expensesFilterCategory.value || '';
                const selectedCategoryId = selectedCategory && selectedCategory !== '__income__' ? Number(selectedCategory) : null;
                const query = (expensesFilterSearch.value || '').trim().toLowerCase();
                const startDate = expensesFilterStart.value ? new Date(`${expensesFilterStart.value}T00:00:00`) : null;
                const endDate = expensesFilterEnd.value ? new Date(`${expensesFilterEnd.value}T23:59:59`) : null;

                filteredExpensesPage = allTransactionsCache.filter(expense => {
                    const expenseDate = new Date(expense.date);
                    if (startDate && expenseDate < startDate) return false;
                    if (endDate && expenseDate > endDate) return false;
                    if (selectedCategory === '__income__' && expense.entryType !== 'income') return false;
                    if (selectedCategoryId) {
                        if (expense.entryType !== 'expense') return false;
                        if (Number(expense.category?.id) !== selectedCategoryId) return false;
                    }

                    if (query) {
                        const haystack = [
                            expense.description || '',
                            expense.category?.name || '',
                            expense.subcategory?.name || '',
                            expense.entryType === 'income' ? 'доход' : 'расход',
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
                allExpensesCache = expenses
                    .sort((a, b) => new Date(b.date) - new Date(a.date))
                    .map(expense => ({ ...expense, entryType: 'expense' }));
            };

            const refreshAllTransactions = async () => {
                const response = await apiFetch(`${API_URL}/incomes?period=all`);
                if (!response.ok) throw new Error('Не удалось загрузить доходы');
                const incomes = await response.json();
                const incomeEntries = incomes.map(income => ({ ...income, entryType: 'income' }));
                allTransactionsCache = [...allExpensesCache, ...incomeEntries].sort((a, b) => {
                    const dateDiff = new Date(b.date) - new Date(a.date);
                    if (dateDiff !== 0) return dateDiff;
                    return Number(b.id || 0) - Number(a.id || 0);
                });
                renderRecentExpenses();
                filterExpensesPage();
            };

            const buildSubcategoryColors = (summaryData) => {
                return summaryData.map((item, index) => getSubcategoryTint(item.categoryColor || '#95a5a6', index));
            };

            const renderExpenseChart = (summaryData, mode, titleText) => {
                if (expenseChart) expenseChart.destroy();
                selectedChartMode = mode;
                const chartFont = getChartFontSizes();
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
                            legend: { position: 'top', labels: { font: { size: chartFont.legend } } },
                            title: { display: true, text: titleText, font: { size: chartFont.title } }
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
                            ctx.font = `600 ${chartFont.center}px Arial`;
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
                    await refreshAllTransactions();
                    if (currentViewId === 'calendar-view') {
                        await renderCalendarView();
                    }
                    if (dayChartDate && calendarDayChartModal.style.display === 'block') {
                        renderDayExpenseChart(dayChartDate);
                    }
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
                        body: JSON.stringify({
                            qrCodeData: qrData,
                            apiToken: receiptApiTokenValue || null,
                        }),
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

            const updateRecurringUiState = (type) => {
                const isExpense = type === 'expense';
                const enabled = isExpense ? expenseRecurringEnabled.checked : incomeRecurringEnabled.checked;
                const options = isExpense ? expenseRecurringOptions : incomeRecurringOptions;
                const period = isExpense ? expenseRecurringPeriod : incomeRecurringPeriod;
                const customDays = isExpense ? expenseRecurringCustomDays : incomeRecurringCustomDays;
                options.classList.toggle('hidden', !enabled);
                customDays.classList.toggle('hidden', period.value !== 'custom');
            };

            expenseRecurringEnabled.addEventListener('change', () => updateRecurringUiState('expense'));
            incomeRecurringEnabled.addEventListener('change', () => updateRecurringUiState('income'));
            expenseRecurringPeriod.addEventListener('change', () => updateRecurringUiState('expense'));
            incomeRecurringPeriod.addEventListener('change', () => updateRecurringUiState('income'));

            addExpenseBtn.addEventListener('click', () => {
                expenseModalTitle.textContent = 'Добавить новый расход';
                expenseIdInput.value = '';
                expenseErrorDiv.textContent = '';
                expenseForm.reset();
                newSubcategoryNameInput.value = '';
                expenseDateInput.value = getTodayDateString();
                expenseAmountInput.readOnly = false;
                expenseAmountHint.classList.add('hidden');
                expenseRecurringEnabled.checked = false;
                expenseRecurringPeriod.value = '1';
                expenseRecurringCustomDays.value = '';
                updateRecurringUiState('expense');
                selectDefaultExpenseCategory();
                expenseModal.style.display = 'block';
                expenseAmountInput.focus();
            });

            addIncomeBtn.addEventListener('click', () => {
                incomeModalTitle.textContent = 'Добавить новый доход';
                incomeIdInput.value = '';
                incomeErrorDiv.textContent = '';
                incomeForm.reset();
                incomeDateInput.value = getTodayDateString();
                incomeRecurringEnabled.checked = false;
                incomeRecurringPeriod.value = '1';
                incomeRecurringCustomDays.value = '';
                updateRecurringUiState('income');
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
                const payload = {
                    amount: expenseAmountInput.value,
                    categoryId: expenseCategorySelect.value,
                    subcategoryId: expenseSubcategorySelect.value || null,
                    description: expenseDescriptionInput.value,
                    date: expenseDateInput.value,
                };

                try {
                    const response = await apiFetch(url, {
                        method,
                        body: JSON.stringify(payload),
                    });

                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({}));
                        expenseErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                        return;
                    }

                    if (!id && expenseRecurringEnabled.checked) {
                        const intervalDays = getRecurringIntervalDays(expenseRecurringPeriod.value, expenseRecurringCustomDays.value);
                        if (!intervalDays) {
                            expenseErrorDiv.textContent = 'Укажите корректную периодичность повторения.';
                            return;
                        }
                        addRecurringRule({
                            type: 'expense',
                            intervalDays,
                            lastRunDate: payload.date,
                            payload: {
                                amount: payload.amount,
                                categoryId: payload.categoryId,
                                subcategoryId: payload.subcategoryId,
                                description: payload.description,
                            },
                        });
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
                const payload = {
                    amount: incomeAmountInput.value,
                    description: incomeDescriptionInput.value,
                    date: incomeDateInput.value,
                };
                try {
                    const response = await apiFetch(url, {
                        method,
                        body: JSON.stringify(payload),
                    });
                    if (!response.ok) {
                        const errorData = await response.json().catch(() => ({}));
                        incomeErrorDiv.textContent = `Ошибка: ${errorData.message || 'Неизвестная ошибка'}`;
                        return;
                    }

                    if (!id && incomeRecurringEnabled.checked) {
                        const intervalDays = getRecurringIntervalDays(incomeRecurringPeriod.value, incomeRecurringCustomDays.value);
                        if (!intervalDays) {
                            incomeErrorDiv.textContent = 'Укажите корректную периодичность повторения.';
                            return;
                        }
                        addRecurringRule({
                            type: 'income',
                            intervalDays,
                            lastRunDate: payload.date,
                            payload: {
                                amount: payload.amount,
                                description: payload.description,
                            },
                        });
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
                const reader = new FileReader();
                reader.onload = (e) => {
                    const result = typeof e.target?.result === 'string' ? e.target.result : '';
                    if (!result.startsWith('data:image/')) {
                        receiptErrorDiv.textContent = 'Ошибка: загруженный файл не является изображением.';
                        receiptPreviewContainer.style.display = 'none';
                        return;
                    }
                    receiptPreview.src = result;
                    receiptPreviewContainer.style.display = 'block';
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
                    img.src = result;
                };
                reader.onerror = () => { receiptErrorDiv.textContent = 'Ошибка: Не удалось прочитать файл.'; };
                reader.readAsDataURL(file);
            });

            const handleExpenseListActions = async (event) => {
                const target = event.target.closest('button');
                if (!target) return;
                const id = target.dataset.id;
                const type = target.dataset.type;
                if (type !== 'expense' && type !== 'income') return;
                const endpoint = `${API_URL}/${getApiPathByType(type)}/${id}`;

                if (target.classList.contains('delete-btn')) {
                    if (!confirm(type === 'income'
                        ? 'Вы уверены, что хотите удалить этот доход?'
                        : 'Вы уверены, что хотите удалить эту запись о расходе?')) return;
                    await apiFetch(endpoint, { method: 'DELETE' });
                    await updateDashboard();
                }

                if (target.classList.contains('edit-btn')) {
                    const response = await apiFetch(endpoint);
                    if (!response.ok) return;
                    const data = await response.json();
                    if (type === 'income') openIncomeEditForm(data);
                    else openExpenseEditForm(data);
                }

                if (target.classList.contains('repeat-btn')) {
                    const response = await apiFetch(endpoint);
                    if (!response.ok) return;
                    const data = await response.json();
                    if (type === 'income') {
                        incomeModalTitle.textContent = 'Повторить доход';
                        incomeIdInput.value = '';
                        incomeAmountInput.value = data.amount;
                        incomeDateInput.value = getTodayDateString();
                        incomeDescriptionInput.value = data.description || '';
                        incomeRecurringEnabled.checked = false;
                        incomeRecurringPeriod.value = '1';
                        incomeRecurringCustomDays.value = '';
                        updateRecurringUiState('income');
                        incomeErrorDiv.textContent = '';
                        incomeModal.style.display = 'block';
                        incomeAmountInput.focus();
                    } else {
                        expenseModalTitle.textContent = 'Повторить расход';
                        expenseIdInput.value = ''; // new record
                        expenseAmountInput.value = data.amount;
                        expenseDateInput.value = getTodayDateString();
                        expenseDescriptionInput.value = data.description || '';
                        expenseCategorySelect.value = data.category?.id || '';
                        populateSubcategories(data.subcategory?.id);
                        expenseAmountInput.readOnly = false;
                        expenseAmountHint.classList.add('hidden');
                        expenseRecurringEnabled.checked = false;
                        expenseRecurringPeriod.value = '1';
                        expenseRecurringCustomDays.value = '';
                        updateRecurringUiState('expense');
                        expenseErrorDiv.textContent = '';
                        expenseModal.style.display = 'block';
                        expenseAmountInput.focus();
                    }
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

            const runAutoSync = async () => {
                if (autoSyncInProgress) return;
                if (initInProgress) return;
                if (document.hidden) return;
                if (!navigator.onLine) return;
                if (!getToken()) return;
                autoSyncInProgress = true;
                try {
                    await updateDashboard();
                } catch (_) {
                    // keep silent for background sync
                } finally {
                    autoSyncInProgress = false;
                }
            };

            const startAutoSync = () => {
                if (autoSyncTimerId) clearInterval(autoSyncTimerId);
                autoSyncTimerId = window.setInterval(() => {
                    runAutoSync().catch(() => {});
                }, AUTO_SYNC_INTERVAL_MS);
            };

            document.addEventListener('visibilitychange', () => {
                if (!document.hidden) {
                    runAutoSync().catch(() => {});
                }
            });

            renderPalettes();
            updateRecurringUiState('expense');
            updateRecurringUiState('income');
            applyPresetDates(expensesPagePreset);
            chartFilterButtons.querySelectorAll('button').forEach(btn => btn.classList.toggle('active', btn.dataset.period === currentChartPeriod));
            expensesFilterPresets.querySelectorAll('button').forEach(btn => btn.classList.toggle('active', btn.dataset.preset === expensesPagePreset));

            await fetchCategories();
            await processRecurringRules();
            await updateDashboard();
            setActiveView('dashboard-view');
            startAutoSync();

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
