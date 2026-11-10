---
title: "Имена и деревья контента"
---

### ${section(2, "sec-names")} Имена и деревья контента

Пусть $\Sigma$ — счётный алфавит *токенов*. **Имя** — конечная последовательность $n = \langle t_{1}, \dots, t_{k}\rangle \in \Sigma^{*}$. Через $\varepsilon$ обозначим пустое имя, через $n\cdot m$ — конкатенацию, через $|n|$ — длину; $n \preccurlyeq m$ означает, что $n$ — префикс $m$ (читается: $m$ лежит в поддереве с корнем $n$). Положим $\mathcal{N} = \Sigma^{*}$.

Пусть $\mathcal{V}$ — множество типизированных скалярных значений. **Значение метаданных** — конечная частичная функция $\mu: \mathcal{N} \rightharpoonup \mathcal{V}$; множество всех таких функций обозначим $\mathcal{M}$. Сами метаданные адресуются именами, поэтому ключ конфигурации может быть путём (`page.title`, `citation.style`), а не плоской строкой.

Пусть $\mathcal{G}(T)$ — множество **целей** (goals) типа $T$: отложенных вычислений, которые при принудительном вычислении дают значение из $T$ или завершаются отказом. Цели — носитель ленивости, и ничто в модели не требует, чтобы цель когда-либо вычислялась.

<div class="theorem def">

**Определение ${label("definition", "def-content-tree")}** (Дерево контента). Деревом контента типа $T$ называется пара $D = (\delta, \beta)$. Здесь $\delta: \mathcal{N} \rightharpoonup \mathcal{M} \times \mathcal{G}(T)$ — конечная частичная функция, сопоставляющая каждому имени листа значение метаданных и цель, а $\beta: \mathcal{N} \rightharpoonup \mathcal{M}$ сопоставляет метаданные именам ветвей; $\operatorname{dom}(\beta)$ замкнуто относительно префиксов и содержит все собственные префиксы всех элементов $\operatorname{dom}(\delta)$. Обозначим через $\mathcal{D}(T)$ множество деревьев контента типа $T$, а через $N = |\operatorname{dom}(\delta)|$ — число листьев.

</div>

Например, для каталога на рисунке ${ref("fig-content-tree")} имя `chapters.intro` — это последовательность из двух токенов $\langle$chapters, intro$\rangle$, и chapters $\preccurlyeq$ chapters.intro. Лист задаётся парой $\delta$(chapters.intro) = $(\mu, g)$, где $\mu$ — метаданные из front matter файла `intro.md`, а $g$ — цель «разобрать `intro.md`», которая ещё не вычислена. Ветвь `chapters` несёт $\beta$(chapters) — метаданные из `@meta.yaml`, а корень $\varepsilon$ — метаданные всего проекта. Знак $\rightharpoonup$ обозначает частичную функцию — словарь, определённый не для всех ключей, а $\operatorname{dom}$ — множество ключей, для которых он определён.

Определение близко к файловой системе: каталоги — имена ветвей, файлы — листья, front matter файла задаёт его $\mu$, а файл метаданных внутри каталога — $\beta$ каталога (рисунок ${ref("fig-content-tree")}). Однако оно файловой системой не ограничено. *Синтетические* узлы без соответствия на диске — указатель, лента, сводная таблица, узел, порождённый посреди конвейера, — обычные элементы $\operatorname{dom}(\delta)$. Для записи исследования именно это позволяет промежуточным и отрицательным результатам, а также интерпретациям быть узлами того же дерева, что и текст статьи, а не файлами рядом с ним (требование Тр1).

<figure class="figure">
<div class="figure-interactive">
<pre class="mermaid">
flowchart TB
  subgraph FS["файловая система"]
    direction LR
    c["content/"] --- ch["chapters/"]
    ch --- meta["@meta.yaml"]
    ch --- intro["intro.md"]
    ch --- res["results.md"]
    c --- fig["figures/"]
    fig --- plot["plot.png"]
  end
  subgraph DT["DataTree⟨Any⟩ после разбора"]
    direction LR
    root(("корень")) --- tch["chapters · ветвь: β из @meta.yaml"]
    tch --- tintro["intro · лист: μ из front matter"]
    tch --- tres["results · лист: μ из front matter"]
    root --- tfig["figures"]
    tfig --- tplot["plot.png · лист: двоичный"]
  end
  FS ==>|"чтение + разбор"| DT
  click meta href "#def-1" "Метаданные ветви β (определение 1)"
  click tintro href "#def-1" "Лист: пара (μ, g)"
  click tres href "#def-1" "Лист: пара (μ, g)"
</pre>
</div>
<div class="figure-static"><img src="${documentRef("figures/content-tree.png")}" alt="Рисунок 1" loading="lazy"></div>
<figcaption>

**Рисунок ${label("figure", "fig-content-tree")}.** Отображение дерева каталогов файловой системы в дерево контента в том виде, в каком его выполняет SNARK. Каждый файл становится типизированным листом, метаданные которого берутся из его front matter. Каталог становится ветвью, метаданные которой берутся из файла `@meta`; имена, начинающиеся с `@`, зарезервированы для таких файлов. Разобранные листья теряют расширение файла, двоичные его сохраняют.

</figcaption>
</figure>
