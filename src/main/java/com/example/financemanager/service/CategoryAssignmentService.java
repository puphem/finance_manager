package com.example.financemanager.service;

import com.example.financemanager.entity.Category;
import com.example.financemanager.entity.Expense;
import com.example.financemanager.entity.Subcategory;
import com.example.financemanager.entity.User;
import com.example.financemanager.repository.CategoryRepository;
import com.example.financemanager.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryAssignmentService {
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}\\s]+");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    public void assignCategory(Expense expense, User user) {
        Category category = suggestCategory(expense.getDescription(), user);
        expense.setCategory(category);
        expense.setSubcategory(suggestSubcategory(expense.getDescription(), category, user));
    }

    public void assignCategoryByReceiptItems(Expense expense, List<String> itemNames, User user) {
        String baseDescription = expense.getDescription();
        String itemText = itemNames == null ? "" : String.join(" ", itemNames);
        String signalText = (baseDescription == null ? "" : baseDescription + " ") + itemText;
        Category category = suggestCategory(signalText, user);
        expense.setCategory(category);
        expense.setSubcategory(suggestSubcategory(signalText, category, user));
    }

    public Category suggestCategory(String expenseDescription, User user) {
        String itemName = normalize(expenseDescription);
        List<Category> categories = categoryRepository.findAllByUser(user);
        Category defaultCategory = categories.stream()
                .filter(category -> "продукты".equalsIgnoreCase(category.getName()))
                .findFirst()
                .orElse(categories.stream().findFirst().orElse(null));

        if (defaultCategory == null) {
            throw new IllegalStateException("В базе данных нет категорий для назначения.");
        }

        Map<String, List<String>> categoryKeywords = buildCategoryKeywords();
        Category matchedCategory = null;
        int bestScore = 0;

        for (Category category : categories) {
            String categoryName = category.getName() == null ? "" : category.getName().toLowerCase(Locale.ROOT);
            int score = 0;

            for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
                if (categoryName.contains(entry.getKey())) {
                    score += countKeywordMatches(itemName, entry.getValue());
                }
            }

            if (!categoryName.isBlank() && itemName.contains(categoryName)) {
                score = Math.max(score, 1);
            }

            if (score > bestScore) {
                bestScore = score;
                matchedCategory = category;
            }
        }

        return matchedCategory != null ? matchedCategory : defaultCategory;
    }

    public Subcategory suggestSubcategory(String expenseDescription, Category category, User user) {
        if (category == null) {
            return null;
        }

        List<Subcategory> subcategories = subcategoryRepository.findByCategoryIdAndCategoryUser(category.getId(), user);
        if (subcategories.isEmpty()) {
            return null;
        }

        String description = normalize(expenseDescription);
        Map<String, List<String>> keywordBySubcategory = buildSubcategoryKeywords();

        Subcategory matchedSubcategory = null;
        int bestScore = 0;

        for (Subcategory subcategory : subcategories) {
            String subcategoryName = subcategory.getName() == null ? "" : subcategory.getName().toLowerCase(Locale.ROOT);
            List<String> keywords = keywordBySubcategory.getOrDefault(subcategoryName, List.of());
            int score = countKeywordMatches(description, keywords);

            if (!subcategoryName.isBlank() && description.contains(subcategoryName)) {
                score = Math.max(score, 1);
            }

            if (score > bestScore) {
                bestScore = score;
                matchedSubcategory = subcategory;
            }
        }

        if (matchedSubcategory != null) {
            return matchedSubcategory;
        }

        return subcategories.stream()
                .filter(subcategory -> "прочее".equalsIgnoreCase(subcategory.getName()))
                .findFirst()
                .orElse(subcategories.get(0));
    }

    private int countKeywordMatches(String sourceText, List<String> keywords) {
        if (sourceText.isBlank() || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int score = 0;
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isBlank() && sourceText.contains(normalizedKeyword)) {
                score++;
            }
        }
        return score;
    }

    private String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String lowered = text.toLowerCase(Locale.ROOT).replace('ё', 'е');
        String noPunctuation = NON_ALNUM.matcher(lowered).replaceAll(" ");
        return MULTIPLE_SPACES.matcher(noPunctuation).replaceAll(" ").trim();
    }

    private Map<String, List<String>> buildCategoryKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("продукт", List.of("молок", "сыр", "хлеб", "яйц", "кефир", "масло", "колбас", "мясо", "овощ", "фрукт", "чай", "кофе", "сахар", "круп", "макарон", "магазин"));
        keywords.put("транспорт", List.of("бенз", "дизел", "метро", "такси", "автобус", "троллейбус", "электричк", "парковк", "каршеринг", "топлив", "uber", "yandex go", "яндекс go", "перевозк", "пассажир", "багаж"));
        keywords.put("такси", List.of("такси", "uber", "taxi", "yandex go", "яндекс go", "поездка", "перевозк", "пассажир", "багаж"));
        keywords.put("кафе", List.of("кафе", "ресторан", "кофейн", "пицц", "бургер", "суши", "доставка"));
        keywords.put("ресторан", List.of("кафе", "ресторан", "кофейн", "пицц", "бургер", "суши", "доставка"));
        keywords.put("развлеч", List.of("кино", "театр", "игр", "билет", "квест", "музей", "концерт", "подписк"));
        keywords.put("счет", List.of("жкх", "коммунал", "интернет", "электр", "вода", "газ", "аренд", "штраф", "налог", "капремонт", "мобильн", "связь"));
        keywords.put("дом", List.of("ремонт", "дом", "мебел", "ламп", "хозтовар", "быт"));
        keywords.put("образов", List.of("курс", "школ", "универс", "книг", "обучен", "репетитор"));
        keywords.put("одеж", List.of("куртк", "футболк", "брюк", "кроссовк", "ботинк", "джинс"));
        keywords.put("красот", List.of("салон", "маникюр", "парикмах", "космет"));
        keywords.put("питом", List.of("вет", "корм", "зоомаг", "питом"));
        keywords.put("здоров", List.of("аптек", "лекар", "витамин", "клиник", "мед"));
        return keywords;
    }

    private Map<String, List<String>> buildSubcategoryKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("напитки", List.of("вода", "сок", "лимонад", "чай", "кофе", "энергетик", "квас"));
        keywords.put("мясо и рыба", List.of("мяс", "кур", "говяд", "свин", "рыб", "лосос", "тунец"));
        keywords.put("молочные продукты", List.of("молок", "сыр", "йогурт", "кефир", "творог", "сметан"));
        keywords.put("сладости", List.of("шоколад", "конфет", "печень", "торт", "морожен", "батончик"));
        keywords.put("овощи и фрукты", List.of("яблок", "банан", "апельсин", "помидор", "огурец", "картоф", "фрукт", "овощ"));
        keywords.put("бытовые продукты", List.of("порош", "моющ", "губк", "мыло", "салфет", "туалет", "хоз"));

        keywords.put("общественный транспорт", List.of("метро", "автобус", "трамвай", "троллейбус", "электричк"));
        keywords.put("топливо", List.of("бенз", "дизел", "азс", "топлив"));
        keywords.put("парковка", List.of("парковк", "паркинг"));
        keywords.put("каршеринг", List.of("каршеринг", "delimobil", "belka", "youdrive"));
        keywords.put("обслуживание авто", List.of("шиномонтаж", "мойка", "то", "масло", "ремонт авто"));

        keywords.put("поездки по городу", List.of("такси", "uber", "yandex go", "яндекс go", "перевозк", "пассажир", "багаж"));
        keywords.put("межгород", List.of("межгород", "поездка между", "дальняя поездка"));
        keywords.put("доставка", List.of("доставка", "courier", "курьер"));
        keywords.put("комфорт/бизнес", List.of("comfort", "business", "комфорт", "бизнес"));

        keywords.put("кино и сериалы", List.of("кино", "cinema", "ivi", "okko", "кинопоиск"));
        keywords.put("игры", List.of("steam", "playstation", "xbox", "игр"));
        keywords.put("концерты", List.of("концерт", "билет"));
        keywords.put("хобби", List.of("хобби", "настольн", "музей", "театр", "квест"));
        keywords.put("подписки", List.of("подписк", "subscription", "youtube premium", "spotify"));

        keywords.put("жкх", List.of("жкх", "коммунал", "вода", "газ", "электр"));
        keywords.put("интернет и связь", List.of("интернет", "связь", "мобильн", "оператор"));
        keywords.put("налоги и штрафы", List.of("налог", "штраф", "госпошлин"));
        keywords.put("аренда/ипотека", List.of("аренд", "ипотек", "rent"));

        keywords.put("ремонт", List.of("ремонт", "краск", "инструмент", "дрель"));
        keywords.put("мебель", List.of("мебел", "диван", "стул", "шкаф"));
        keywords.put("бытовая химия", List.of("порош", "чист", "химия", "моющ"));
        keywords.put("техника", List.of("пылесос", "чайник", "микроволн", "техник"));
        keywords.put("хозтовары", List.of("хозтовар", "ламп", "батарейк", "удлинитель"));

        keywords.put("аптека", List.of("аптек", "лекар", "таблет", "витамин"));
        keywords.put("врачи и анализы", List.of("клиник", "анализ", "врач", "узи"));
        keywords.put("стоматология", List.of("стомат", "зуб", "брекет"));
        keywords.put("спорт и здоровье", List.of("фитнес", "спортзал", "йога", "бассейн"));

        keywords.put("повседневная одежда", List.of("футболк", "рубашк", "брюк", "джинс"));
        keywords.put("обувь", List.of("кроссовк", "ботинк", "туфл", "обув"));
        keywords.put("аксессуары", List.of("ремень", "сумк", "рюкзак", "шапк"));
        keywords.put("спорттовары", List.of("спорт", "форма", "ракетк", "мяч"));

        keywords.put("кафе", List.of("кафе", "кофейн"));
        keywords.put("рестораны", List.of("ресторан", "бар", "гриль"));
        keywords.put("фастфуд", List.of("бургер", "шаверм", "пицц", "fast"));
        keywords.put("доставка еды", List.of("доставка", "delivery", "самокат", "яндекс еда"));
        keywords.put("кофейни", List.of("латте", "капучино", "эспрессо", "кофе"));

        keywords.put("курсы", List.of("курс", "обучен", "интенсив"));
        keywords.put("книги", List.of("книг", "учебник"));
        keywords.put("репетиторы", List.of("репетитор", "урок"));
        keywords.put("онлайн-платформы", List.of("udemy", "coursera", "stepik", "skillbox"));

        keywords.put("корм", List.of("корм", "лакомств"));
        keywords.put("ветклиника", List.of("вет", "ветеринар", "прививк"));
        keywords.put("аксессуары для питомцев", List.of("поводок", "лоток", "наполнитель", "миска"));
        keywords.put("груминг", List.of("грум", "стрижк", "уход за питомцем"));

        keywords.put("косметика", List.of("космет", "крем", "маск"));
        keywords.put("салон", List.of("салон", "парикмах", "маникюр"));
        keywords.put("уход за собой", List.of("уход", "spa", "массаж"));
        keywords.put("парфюмерия", List.of("духи", "парфюм"));
        return keywords;
    }
}
