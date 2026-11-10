<!-- LLM generated code: описание изменений для примера статьи и движка документов -->
# Описание изменений: пример статьи и движок документов (2026-11-10)

Здесь описаны незакоммиченные изменения в ветке `dev`, сделанные при преобразовании `docs/TemplateArticle.docx` в
пример документа. Работа затронула два модуля движка и приложение `examples/document`.

| Файл                                                   | Тема                                                                     |
|--------------------------------------------------------|--------------------------------------------------------------------------|
| [01-snark-html.md](01-snark-html.md)                   | Чтение Markdown и постпроцессор: четыре исправления и новое поведение    |
| [02-snark-documents.md](02-snark-documents.md)         | Нумерация, перекрёстные ссылки, поля дескриптора, статические файлы      |
| [03-article-structure.md](03-article-structure.md)     | Структура статьи и спецификация её мета-файлов                           |
| [04-authoring-guide.md](04-authoring-guide.md)         | Как писать раздел, определение, рисунок, таблицу, листинг, цитату        |
| [05-design-and-example-app.md](05-design-and-example-app.md) | Оформление страницы и изменения в `examples/document/main.kt`      |
| [06-docx-conversion.md](06-docx-conversion.md)         | Как был преобразован docx и что проверить вручную                        |
| [07-known-issues.md](07-known-issues.md)               | Изменения поведения, открытые ошибки и ограничения                       |

## Кратко

* **Статья.** `examples/document/data/article` содержит разделы 4–5 и список литературы статьи о SNARK. Она написана
  на Markdown с формулами LaTeX, по одному файлу на подраздел и по папке на главу. Номера разделов, определений,
  предложений, рисунков, таблиц, листингов и формул генерирует движок.
* **snark-html.** Ссылки Markdown указывали на собственный текст, ссылки изображений не переписывались, а вывод
  шаблонов в тексте Markdown экранировался. Все три ошибки исправлены. Выражения `${...}` теперь непрозрачны для
  Markdown, а текст внутри `<code>` никогда не обрабатывается.
* **snark-documents.** Метки могут иметь ключи, `${ref("key")}` выводит номер помеченного объекта (в том числе
  определённого ниже), нумерация остаётся сквозной по всему документу, дескрипторы могут подключать
  таблицы стилей и скрипты, а файлы в подкаталогах сохраняют свой путь.

## Изменённые файлы

```
snark-html/src/jvmMain/kotlin/space/kscience/snark/html/MarkdownReader.kt
snark-html/src/jvmMain/kotlin/space/kscience/snark/html/Postprocessor.kt
snark-html/docs/links.md
snark-documents/src/jvmMain/kotlin/FtlDocumentProcessor.kt
snark-documents/src/jvmMain/kotlin/DocumentBuilder.kt
snark-documents/src/jvmMain/kotlin/DocumentDescriptor.kt
snark-documents/docs/README-TEMPLATE.md
examples/document/src/jvmMain/kotlin/main.kt
examples/document/docs/README-TEMPLATE.md
examples/document/data/article/**          (новые)
CHANGELOG.md                               (новый)
```

## Проверка результата

```shell
./gradlew :examples:document:run
```

Откройте http://localhost:8080/article. Статья проверена в headless Microsoft Edge: 417 формул отрисованы KaTeX без
ошибок, 5 диаграмм Mermaid, 21 пункт оглавления, 74 идентификатора источников, 21 ссылка-цитата и 18 обратных ссылок.
Сгенерированные номера совпадают с docx (определения 1–13, предложения 1–5, рисунки 1–6, таблицы 3–5, листинги 1–10).
Вручную в обычном браузере статья не проверялась.
