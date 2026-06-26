import os

# --- НАСТРОЙКИ ---
# Директория, в которой находится проект. Точка означает "текущая директория".
PROJECT_ROOT = '.'
# Имя файла, в который будет сохранен результат.
OUTPUT_FILE = 'project_context.txt'
# Расширения файлов, которые мы хотим включить.
INCLUDE_EXTENSIONS = ['.java', '.js', '.html', '.css', '.properties', '.gradle', '.yml', 'Dockerfile']
# Папки, которые нужно проигнорировать.
EXCLUDE_DIRS = ['.gradle', 'build', '.idea', 'out', 'data', '.git']

def collect_project_files():
    """
    Собирает содержимое всех указанных файлов проекта в один текстовый файл.
    """
    print(f"Начинаю сборку файлов проекта в '{OUTPUT_FILE}'...")

    # Открываем файл для записи. 'w' означает, что файл будет перезаписан при каждом запуске.
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as outfile:
        # os.walk рекурсивно обходит все папки и файлы, начиная с корня проекта.
        for root, dirs, files in os.walk(PROJECT_ROOT):

            # Умное исключение папок: мы говорим os.walk даже не заходить в них.
            dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]

            for file in files:
                # Проверяем, заканчивается ли имя файла на одно из нужных нам расширений.
                if any(file.endswith(ext) for ext in INCLUDE_EXTENSIONS):

                    file_path = os.path.join(root, file)
                    # Приводим путь к универсальному виду с /
                    relative_path = os.path.relpath(file_path, PROJECT_ROOT).replace('\\', '/')

                    print(f"  -> Добавляю файл: {relative_path}")

                    # Пишем заголовок с именем файла
                    outfile.write(f"// FILE: {relative_path}\n")
                    outfile.write("=" * 80 + "\n")

                    try:
                        # Открываем и читаем содержимое исходного файла
                        with open(file_path, 'r', encoding='utf-8') as infile:
                            outfile.write(infile.read())
                    except Exception as e:
                        outfile.write(f"!!! ОШИБКА ЧТЕНИЯ ФАЙЛА: {e} !!!\n")

                    # Добавляем несколько пустых строк для разделения
                    outfile.write("\n\n\n")

    print("-" * 30)
    print(f"Готово! Весь код проекта сохранен в файл: {OUTPUT_FILE}")
    print("Теперь вы можете скопировать его содержимое и вставить в чат.")

# --- ЗАПУСК СКРИПТА ---
if __name__ == "__main__":
    collect_project_files()
