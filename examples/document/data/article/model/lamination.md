---
title: "Ламинирование: алгебра слоистых метаданных"
---

### ${section(2, "sec-lamination")} Ламинирование: алгебра слоистых метаданных

Конфигурация исследовательского проекта естественно слоиста: умолчание для всего сайта, умолчание для раздела, переопределение для элемента и, для рукописи, требования издания. Рисунок ${ref("fig-lamination")} показывает это устройство; в этом подразделе формализуется композиция слоёв.

<div class="theorem def">

**Определение ${label("definition", "def-lamination")}** (Ламинирование). Определим $\oplus: \mathcal{M} \times \mathcal{M} \to \mathcal{M}$ так: $(\mu_{1} \oplus \mu_{2})(n) = \mu_{1}(n)$, если $n \in \operatorname{dom}(\mu_{1}); \mu_{2}(n)$, если $n \in \operatorname{dom}(\mu_{2})$ \\ $\operatorname{dom}(\mu_{1})$; иначе значение не определено. Приоритет у левого операнда.

$$
(\mu_1 \oplus \mu_2)(n) = \begin{cases} \mu_1(n), & n \in \operatorname{dom}(\mu_1), \\ \mu_2(n), & n \in \operatorname{dom}(\mu_2) \setminus \operatorname{dom}(\mu_1), \\ \text{не определено} & \text{иначе.} \end{cases} \tag{${label("equation", "eq-lamination")}}
$$

</div>

<div class="theorem prop">

**Предложение ${label("proposition", "prop-lamination-band")}** (Ламинирование — левая регулярная связка). $(\mathcal{M}, \oplus)$ — моноид с единицей $\varnothing$, который для всех $\mu, \nu, \rho \in \mathcal{M}$ удовлетворяет условиям: (1) идемпотентность, $\mu \oplus \mu = \mu$; (2) ассоциативность, $(\mu \oplus \nu) \oplus \rho = \mu \oplus (\nu \oplus \rho)$; (3) левая регулярность, $\mu \oplus \nu \oplus \mu = \mu \oplus \nu$. Операция не коммутативна.

</div>

<div class="proof">

**Доказательство предложения ${ref("prop-lamination-band")}.** (1) и (2) непосредственно следуют из разбора случаев в определении ${ref("def-lamination")}. Для (3) зафиксируем $n$. Если $n \in \operatorname{dom}(\mu)$, обе части дают $\mu(n)$. Если $n \in \operatorname{dom}(\nu)$ \\ $\operatorname{dom}(\mu)$, обе дают $\nu(n)$. Иначе $n$ не лежит ни в одной области, третье вхождение $\mu$ не может внести вклад, так как $n \notin \operatorname{dom}(\mu)$, и обе части не определены. Пустая частичная функция — двусторонняя единица, так что структура — моноид, а не просто полугруппа; единица неявно используется в определении ${ref("def-effective-meta")}, когда стек предков короток. Некоммутативность: возьмём μ = {a ↦ 1} и ν = {a ↦ 2}. □

</div>

Эта операция — функциональное переопределение (override) нотации $Z$ [${ref("ref-spivey1992")}] с обращённым приоритетом: в $Z f \oplus g$ берёт значение $g$ там, где определены обе функции, а здесь побеждает левый операнд. Левая регулярность выделяет левые регулярные связки среди идемпотентных полугрупп [${ref("ref-brown2000")}]. Это алгебраическая форма утверждения, что *повторное наложение слоя, уже присутствующего ниже в стеке, ничего не меняет*.

Отсюда три практических следствия. Во-первых, стек слоёв можно один раз «сплющить» в единое $\mu$ и повторно использовать результат: никакое последующее ламинирование его не изменит. Во-вторых, эффективные метаданные узла не зависят от того, как собран стек. Поэтому два пути в коде, собирающие один и тот же стек с разной группировкой, дают одно и то же, и ламинирование безопасно выставлять как публичный API (application programming interface). В-третьих, сплющенное значение — законный вход ключа кэша (раздел ${ref("sec-evaluation")}), так как оно является функцией одного лишь стека.

**Плоские и древовидные метаданные.** Определение ${ref("def-lamination")} ламинирует плоские частичные функции на именах: иерархия заключена в ключе, а слияние идёт по листьям. Реализация, в которой метаданные — рекурсивное дерево узлов, могла бы вместо этого сливать по узлам, переопределяя целое поддерево, если его корень есть в верхнем слое. Две операции расходятся на случае, который на практике встречается постоянно. Пусть слой сайта задаёт `page.author`, а слой страницы — `page.title`. Слияние по листьям даёт оба ключа, а переопределение узла `page` целиком — только `title`. Предложение ${ref("prop-lamination-band")} доказано для слияния по листьям, и приведённое выше рассуждение о кэшировании опирается на него. Поэтому то, какую операцию даёт реализация, — обязательство по доказательству, а не деталь; для SNARK оно выполняется в разделе ${ref("sec-dataforge")}.

<figure class="figure">
<div class="figure-interactive">
<div class="lam-widget" id="lam-widget">
  <div class="lam-controls">
    <label><input type="checkbox" data-layer="item" checked> front matter элемента</label>
    <label><input type="checkbox" data-layer="dir" checked> умолчания каталога</label>
    <label><input type="checkbox" data-layer="site" checked> слой сайта</label>
    <label><input type="checkbox" data-layer="venue" checked> профиль издания</label>
    <label class="lam-order"><input type="checkbox" id="lam-venue-first"> профиль первым (навязывает требования)</label>
  </div>
  <div class="lam-stack" id="lam-stack"></div>
  <div class="lam-result">
    <div class="lam-result-title">Эффективные метаданные <span class="m">μ̂</span></div>
    <table><thead><tr><th>ключ</th><th>значение</th><th>источник</th></tr></thead><tbody id="lam-out"></tbody></table>
  </div>
</div>
<script>
(function () {
  var layers = {
    item: { title: "front matter элемента", meta: { "page.title": "Results" } },
    dir: { title: "умолчания каталога", meta: { "page.author": "A. N.", "page.title": "Chapter" } },
    site: { title: "слой сайта", meta: { "page.author": "SPC", "numbering": "per-page" } },
    venue: { title: "профиль издания", meta: { "numbering": "global", "citation.style": "acs" } }
  };
  var root = document.getElementById("lam-widget");
  function render() {
    var enabled = {};
    root.querySelectorAll("input[data-layer]").forEach(function (i) { enabled[i.dataset.layer] = i.checked; });
    var order = ["item", "dir", "site", "venue"];
    if (document.getElementById("lam-venue-first").checked) order = ["venue", "item", "dir", "site"];
    order = order.filter(function (k) { return enabled[k]; });
    // left operand wins: the first layer that defines a key gives its value
    var result = {};
    order.forEach(function (k) {
      Object.keys(layers[k].meta).forEach(function (key) {
        if (!(key in result)) result[key] = { value: layers[k].meta[key], from: k };
      });
    });
    var stack = document.getElementById("lam-stack");
    stack.innerHTML = "";
    order.forEach(function (k, idx) {
      if (idx > 0) { var op = document.createElement("span"); op.className = "lam-op"; op.textContent = "⊕"; stack.appendChild(op); }
      var box = document.createElement("div");
      box.className = "lam-layer lam-" + k;
      var h = document.createElement("div"); h.className = "lam-layer-title"; h.textContent = layers[k].title; box.appendChild(h);
      Object.keys(layers[k].meta).forEach(function (key) {
        var row = document.createElement("div");
        row.className = "lam-kv" + (result[key].from === k ? " lam-win" : " lam-lose");
        row.textContent = key + " = \"" + layers[k].meta[key] + "\"";
        box.appendChild(row);
      });
      stack.appendChild(box);
    });
    var out = document.getElementById("lam-out");
    out.innerHTML = "";
    Object.keys(result).sort().forEach(function (key) {
      var tr = document.createElement("tr");
      [key, "\"" + result[key].value + "\"", layers[result[key].from].title].forEach(function (v, i) {
        var td = document.createElement("td"); if (i < 2) td.className = "mono"; td.textContent = v; tr.appendChild(td);
      });
      out.appendChild(tr);
    });
  }
  root.addEventListener("change", render);
  render();
})();
</script>
</div>
<div class="figure-static"><img src="${documentRef("figures/lamination.png")}" alt="Рисунок 2" loading="lazy"></div>
<figcaption>

**Рисунок ${label("figure", "fig-lamination")}.** Ламинирование четырёх слоёв: front matter элемента, умолчания каталога, слой сайта и профиль издания. Слияние идёт по листьям на полных точечных именах, поэтому ключ, присутствующий только в нижнем слое, сохраняется. Профиль издания, стоящий последним, как здесь, задаёт умолчания, которые верхние слои переопределяют; поставленный первым, он навязывал бы свои значения.

</figcaption>
</figure>

<div class="theorem def">

**Определение ${label("definition", "def-effective-meta")}** (Эффективные метаданные). Пусть $D = (\delta, \beta)$ — дерево контента, $n \in \operatorname{dom}(\delta)$ — лист с $\delta(n) = (\mu_{n}, g_{n})$, а $\mu_{x}$ — окружающие метаданные (слои сайта и издания). Эффективными метаданными $n$ называется $\hat{\mu}(n) = \mu_{n} \oplus \beta(p_{k-1}) \oplus \beta(p_{k-2}) \oplus \cdots \oplus \beta(\varepsilon) \oplus \mu_{x}$, где $\varepsilon = p_{0} \prec p_{1} \prec \cdots \prec p_{k-1} \prec n$ перечисляет собственные префиксы $n$ из $\operatorname{dom}(\beta)$.

$$
\hat\mu(n) = \mu_n \oplus \beta(p_{k-1}) \oplus \beta(p_{k-2}) \oplus \cdots \oplus \beta(\varepsilon) \oplus \mu_x \tag{${label("equation", "eq-effective-meta")}}
$$

</div>

По предложению ${ref("prop-lamination-band")} это выражение однозначно и вычисляется за $O(|n|)$ шагов ламинирования. По левой регулярности результат устойчив к повторному ламинированию с любым слоем предка.

**Профили изданий как слои.** Переадресация (проблема П6) в этой модели — изменение окружающего слоя, а не исходника. Требования издания — стиль цитирования, объём аннотации, порядок разделов, правила нумерации — образуют значение метаданных $\mu_{v}$, и эффективные метаданные записи для этого издания получаются ламинированием $\mu_{v}$ в стек. Место $\mu_{v}$ в стеке — проектное решение с точным следствием:

- поставленный *последним*, как в определении ${ref("def-effective-meta")} при $\mu_{x} = \mu$<sub>site</sub> $\oplus \mu_{v}$, он задаёт умолчания, которые автор может переопределить;
- поставленный *первым*, как $\mu_{v} \oplus \hat{\mu}(n)$, он навязывает требования, которые не может переопределить ни один авторский слой.

Оба размещения — ламинирования, и по предложению ${ref("prop-lamination-band")} каждое определено однозначно, как бы ни был собран стек. Следовательно, профиль издания — слой, а не копия исходника. Обязательным правилам журнала место на первой позиции, его умолчаниям — на последней.
