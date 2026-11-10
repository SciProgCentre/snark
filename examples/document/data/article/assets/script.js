// LLM generated code: article behaviour (header, table of contents, figures, listings, citations, return links)
(function () {
  "use strict";

  function el(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text) e.textContent = text;
    return e;
  }

  // ---------- header: title, authors and the parts from summary.md ----------
  var title = document.querySelector("h1.title");
  var parts = document.querySelector(".hero-parts");
  var meta = parts ? parts.dataset : {};
  if (title && parts) {
    var hero = el("header", "hero");
    var inner = el("div", "hero-inner");
    hero.appendChild(inner);
    title.parentNode.insertBefore(hero, title);
    inner.appendChild(parts.querySelector(".eyebrow"));
    inner.appendChild(title);
    inner.appendChild(parts.querySelector(".subtitle"));
    var row = parts.querySelector(".meta-row");
    var list = row.querySelector(".author-list");
    document.querySelectorAll("body > .author").forEach(function (a) {
      var entry = el("div", "author-entry");
      var name = a.querySelector(".author-name");
      var affiliation = a.querySelector(".author-affiliation");
      entry.textContent = name ? name.textContent : a.textContent;
      if (affiliation) entry.appendChild(el("div", "aff", affiliation.textContent));
      list.appendChild(entry);
      a.remove();
    });
    inner.appendChild(row);
    parts.remove();
  }

  // ---------- share: theme, link, citation ----------
  function copy(text, button) {
    var label = button.textContent;
    var done = function () { button.textContent = "Скопировано ✓"; setTimeout(function () { button.textContent = label; }, 1500); };
    if (navigator.clipboard) navigator.clipboard.writeText(text).then(done, function () {});
  }
  var themeToggle = document.getElementById("theme-toggle");
  if (themeToggle) themeToggle.addEventListener("click", function () {
    var root = document.documentElement;
    var dark = root.dataset.theme ? root.dataset.theme === "dark" : window.matchMedia("(prefers-color-scheme: dark)").matches;
    root.dataset.theme = dark ? "light" : "dark";
  });
  var copyLink = document.getElementById("copy-link");
  if (copyLink) copyLink.addEventListener("click", function () { copy(location.href.split("#")[0], copyLink); });

  var authors = Array.prototype.map.call(document.querySelectorAll(".author-entry"), function (e) { return e.firstChild.textContent.trim(); });
  var bibtex = "@article{snark" + (meta.year || "") + ",\n" +
    "  title   = {" + (title ? title.textContent.trim() : "") + "},\n" +
    "  author  = {" + authors.join(" and ") + "},\n" +
    "  journal = {" + (meta.journal || "") + "},\n" +
    "  year    = {" + (meta.year || "") + "},\n" +
    "  volume  = {" + (meta.volume || "") + "},\n" +
    "  doi     = {" + (meta.doi || "") + "}\n}";
  var modal = el("div", "cite-modal");
  modal.setAttribute("role", "dialog");
  var dialog = el("div", "dialog");
  dialog.appendChild(el("h4", "", "Цитирование"));
  dialog.appendChild(el("pre", "", bibtex));
  var actions = el("div", "actions");
  var close = el("button", "", "Закрыть");
  var copyBib = el("button", "primary", "Скопировать BibTeX");
  actions.appendChild(close);
  actions.appendChild(copyBib);
  dialog.appendChild(actions);
  modal.appendChild(dialog);
  document.body.appendChild(modal);
  var openCite = document.getElementById("open-cite");
  if (openCite) openCite.addEventListener("click", function () { modal.classList.add("open"); });
  close.addEventListener("click", function () { modal.classList.remove("open"); });
  copyBib.addEventListener("click", function () { copy(bibtex, copyBib); });
  modal.addEventListener("click", function (e) { if (e.target === modal) modal.classList.remove("open"); });
  document.addEventListener("keydown", function (e) { if (e.key === "Escape") modal.classList.remove("open"); });

  // ---------- reference list: ids and citation previews ----------
  var references = {};
  // each item starts with ${label("reference", "ref-key")}; its key becomes the id of the item
  document.querySelectorAll(".references li").forEach(function (li) {
    var label = li.querySelector("a.snark-label");
    if (!label) return;
    li.id = label.id;
    label.remove();
    references[li.id] = li.textContent.replace(/\s+/g, " ").trim();
  });
  var refHeading = document.querySelector(".references");
  if (refHeading && refHeading.previousElementSibling && refHeading.previousElementSibling.tagName === "H2") {
    refHeading.previousElementSibling.classList.add("references-heading");
    refHeading.previousElementSibling.id = "references";
  }
  document.querySelectorAll('a[href^="#ref-"]').forEach(function (a) {
    a.classList.add("cite");
    var text = references[a.getAttribute("href").slice(1)];
    if (text) a.dataset.ref = text;
  });

  // ---------- table of contents from the numbered headings ----------
  var toc = el("nav", "toc");
  toc.setAttribute("aria-label", "Содержание");
  toc.appendChild(el("div", "toc-title", "Содержание"));
  var ol = el("ol");
  document.querySelectorAll("body > h2, body > h3").forEach(function (h) {
    var number = h.querySelector("a.snark-section");
    if (!h.id) h.id = number ? "h-" + number.id : "h-" + Math.random().toString(36).slice(2);
    var li = el("li", h.tagName === "H2" ? "toc-l2" : "toc-l3");
    var a = el("a");
    a.href = "#" + h.id;
    if (number) a.appendChild(el("span", "secnum", number.textContent));
    a.appendChild(document.createTextNode(" " + h.textContent.replace(number ? number.textContent : "", "").trim()));
    li.appendChild(a);
    ol.appendChild(li);
  });
  toc.appendChild(ol);
  var firstHeading = document.querySelector("body > h2");
  if (firstHeading) firstHeading.parentNode.insertBefore(toc, firstHeading);

  // ---------- listings: line numbers ----------
  document.querySelectorAll("figure.listing pre > code").forEach(function (code) {
    var lines = code.textContent.replace(/\n$/, "").split("\n");
    code.textContent = "";
    lines.forEach(function (line) { code.appendChild(el("span", "ln", line)); });
  });

  // ---------- figures: interactive and static views ----------
  document.querySelectorAll("figure.figure").forEach(function (fig) {
    if (!fig.querySelector(".figure-interactive")) return;
    fig.classList.add("has-interactive");
    var tabs = el("div", "figure-tabs");
    [["Интерактивная схема", false], ["Исходный рисунок", true]].forEach(function (t) {
      var b = el("button", t[1] ? "" : "on", t[0]);
      b.type = "button";
      b.addEventListener("click", function () {
        fig.classList.toggle("show-static", t[1]);
        tabs.querySelectorAll("button").forEach(function (x) { x.classList.toggle("on", x === b); });
      });
      tabs.appendChild(b);
    });
    fig.insertBefore(tabs, fig.firstChild);
  });

  // ---------- return links: citations and cross-references ----------
  var uses = {};
  document.querySelectorAll("a.cite, a.snark-ref").forEach(function (a) {
    var target = a.getAttribute("href").slice(1);
    uses[target] = uses[target] || [];
    if (!a.id) a.id = "from-" + target + "-" + (uses[target].length + 1);
    uses[target].push(a.id);
  });
  document.querySelectorAll(".references li").forEach(function (li) {
    var from = uses[li.id];
    if (!from) return;
    var span = el("span", "backlinks");
    from.forEach(function (id, i) {
      var b = el("a", "", from.length === 1 ? "↩" : "↩" + (i + 1));
      b.href = "#" + id;
      b.title = "Вернуться к месту цитирования";
      span.appendChild(b);
    });
    li.appendChild(span);
  });
  var back = el("a", "back-to-text", "↩ Назад к тексту");
  back.title = "Вернуться к месту, откуда был совершён переход";
  document.body.appendChild(back);
  document.addEventListener("click", function (e) {
    var link = e.target.closest ? e.target.closest("a") : null;
    if (!link) return;
    if (link.matches("a.cite, a.snark-ref")) {
      back.href = "#" + link.id;
      back.classList.add("shown");
    } else if (link === back || link.closest(".backlinks")) {
      back.classList.remove("shown");
    }
  });

  // ---------- reading progress, sidebar visibility, active section ----------
  var bar = el("div", "progress");
  document.body.appendChild(bar);
  var heroEl = document.querySelector(".hero");
  var links = Array.prototype.slice.call(toc.querySelectorAll("a"));
  var targets = links.map(function (a) { return document.getElementById(a.getAttribute("href").slice(1)); });
  function onScroll() {
    var h = document.documentElement;
    bar.style.width = (100 * h.scrollTop / Math.max(1, h.scrollHeight - h.clientHeight)) + "%";
    if (heroEl) toc.classList.toggle("visible", heroEl.getBoundingClientRect().bottom < 80);
    var current = 0;
    targets.forEach(function (t, i) { if (t && t.getBoundingClientRect().top < 120) current = i; });
    links.forEach(function (a, i) { a.classList.toggle("active", i === current); });
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
})();
