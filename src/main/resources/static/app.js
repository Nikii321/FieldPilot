"use strict";
const $ = (s) => document.querySelector(s),
  esc = (x) =>
    String(x ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
const fmt = (x, d = 2) =>
  x == null
    ? "—"
    : Number(x).toLocaleString("ru-RU", {
        maximumFractionDigits: d,
        minimumFractionDigits: d,
      });
const pages = [
  ["overview", "Обзор месторождения"],
  ["wells", "Скважины и мероприятия"],
  ["ahp", "Приоритеты · AHP"],
  ["scenarios", "Риски и сценарии"],
  ["portfolio", "Распределение бюджета"],
  ["knowledge", "Экспертные правила"],
  ["ml", "Прогноз и обучение"],
  ["recommendations", "Рекомендации"],
  ["decisions", "Журнал решений"],
];
let analysis = null,
  request = { budget: 3.5, crewDays: 12 },
  page = "overview";
$("#nav").innerHTML = pages
  .map(([k, v]) => `<a href="#${k}" data-page="${k}">${v}</a>`)
  .join("");
async function api(path, method = "GET", body) {
  const r = await fetch("/api" + path, {
    method,
    headers: body !== undefined ? { "Content-Type": "application/json" } : {},
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await r.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }
  if (!r.ok) throw Error(data.error || `Ошибка HTTP ${r.status}`);
  return data;
}
function error(e) {
  $("#notice").className = "error";
  $("#notice").textContent = e.message || String(e);
}
function clearError() {
  $("#notice").textContent = "";
  $("#notice").className = "";
}
function button(label, attrs = "", secondary = true) {
  return `<button class="${secondary ? "secondary " : ""}tiny" ${attrs}>${label}</button>`;
}
function metric(label, value, note) {
  return `<div class="metric"><span>${label}</span><strong>${value}</strong><small>${note}</small></div>`;
}
function card(title, body, extra = "") {
  return `<div class="card ${extra}"><h2>${title}</h2>${body}</div>`;
}
function table(headers, rows) {
  return `<table><thead><tr>${headers.map((h) => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows.map((r) => `<tr>${r.map((c) => `<td>${c}</td>`).join("")}</tr>`).join("")}</tbody></table>`;
}
function bars(rows) {
  const max = Math.max(...rows.map((r) => Math.abs(r[1])), 1e-9);
  return rows
    .map(
      ([name, v, suffix]) =>
        `<div class="barrow"><span>${esc(name)}</span><div class="bartrack"><div class="barfill" style="width:${(Math.abs(v) / max) * 100}%;background:${v < 0 ? "#d57952" : "#109e8a"}"></div></div><b>${fmt(v)}${suffix || ""}</b></div>`,
    )
    .join("");
}
function download(name, text, type = "application/json") {
  const u = URL.createObjectURL(new Blob([text], { type }));
  const a = document.createElement("a");
  a.href = u;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(u), 1000);
}
async function calculate() {
  clearError();
  $("#recalculate").disabled = true;
  $("#status").textContent = "Выполняется расчет…";
  try {
    request.budget = Number($("#budget").value);
    request.crewDays = Number($("#crew").value);
    analysis = await api("/analysis", "POST", request);
    request = analysis.request;
    $("#status").textContent =
      "Расчет обновлен · " + new Date().toLocaleTimeString("ru-RU");
    await render();
  } catch (e) {
    analysis = null;
    $("#content").innerHTML = card(
      "Расчет недоступен",
      "<p>Исправьте параметры или данные. Карточки скважин и модель доступны через меню.</p>",
    );
    error(e);
  } finally {
    $("#recalculate").disabled = false;
  }
}
function openDialog(title, body, onSave, saveText = "Сохранить") {
  $("#dialogtitle").textContent = title;
  $("#dialogbody").innerHTML = body;
  $("#dialogerror").textContent = "";
  $("#dialogsave").textContent = saveText;
  $("#dialogsave").hidden = !onSave;
  $("#dialogform").onsubmit = async (e) => {
    e.preventDefault();
    $("#dialogsave").disabled = true;
    try {
      await onSave();
      $("#dialog").close();
    } catch (err) {
      $("#dialogerror").textContent = err.message;
    } finally {
      $("#dialogsave").disabled = false;
    }
  };
  $("#dialog").showModal();
}
$("#close").onclick = () => $("#dialog").close();
$("#recalculate").onclick = calculate;
$("#export").onclick = () =>
  analysis &&
  download("fieldpilot-analysis.json", JSON.stringify(analysis, null, 2));
window.addEventListener("hashchange", () => render().catch(error));
async function render() {
  page = location.hash.slice(1) || "overview";
  if (!pages.some((p) => p[0] === page)) page = "overview";
  $("#title").textContent = pages.find((p) => p[0] === page)[1];
  document
    .querySelectorAll("nav a")
    .forEach((a) => a.classList.toggle("active", a.dataset.page === page));
  clearError();
  const content = $("#content");
  if (page === "wells") return renderWells();
  if (page === "ml") return renderML();
  if (page === "decisions") return renderDecisions();
  if (page === "knowledge") return renderKnowledge();
  if (!analysis) {
    content.innerHTML = card(
      "Нет актуального расчета",
      "<p>Нажмите «Пересчитать портфель». Проверьте допуски, данные и ограничения.</p>",
    );
    return;
  }
  const a = analysis,
    p = a.plan;
  if (page === "overview") {
    content.innerHTML = `<div class="metrics">${metric("Ожидаемый эффект", fmt(p.expectedEffect) + " млн", "Дополнительно к базовой стратегии")}${metric("Добыча · центральный", fmt(p.centralProduction, 0) + " т", "За 30 суток; базовая " + fmt(p.baselineProduction, 0) + " т")}${metric("Использован бюджет", fmt(p.cost) + " млн", fmt(request.budget - p.cost) + " млн ₽ свободно")}${metric("Бригадо-дни", fmt(p.crewDays, 0) + " / " + request.crewDays, "Обязательные действия включены")}</div><div class="grid">${card(
      "Приоритет внимания",
      bars(
        a.wells
          .filter((x) => x.priority != null)
          .sort((x, y) => y.priority - x.priority)
          .map((w) => [w.well.name, w.priority * 100, "%"]),
      ),
    )}${card(
      "План на 30 дней",
      table(
        ["Скважина", "Действие", "Бюджет, млн ₽"],
        p.actions.map((o) => [
          esc(o.wellName),
          esc(o.action.title),
          fmt(o.action.cost),
        ]),
      ),
    )}${card("Как получается решение", '<div class="flow"><div>1. Данные<br><small>Реестр и допуски</small></div><div>2. Прогноз<br><small>Базовая добыча</small></div><div>3. Приоритет<br><small>Согласованный AHP</small></div><div>4. Портфель<br><small>Бюджет и бригады</small></div></div>', "wide")}</div><div class="callout warn" style="margin-top:20px">${esc(a.warnings[0])} ${esc(a.warnings[1])}</div>`;
  }
  if (page === "portfolio") {
    content.innerHTML = card(
      "Оптимальный портфель",
      `<div class="callout">Точный перебор с отсечениями: ${p.exploredNodes} узлов. Максимизируется ожидаемый дополнительный экономический эффект. Для каждой скважины выбрано ровно одно действие.</div>${table(
        [
          "Скважина",
          "Действие",
          "Прирост, т",
          "Стоимость, млн ₽",
          "Бригадо-дни",
          "Эффект, млн ₽",
        ],
        p.actions.map((o) => [
          esc(o.wellName),
          esc(o.action.title),
          fmt(o.gainTonnes, 0),
          fmt(o.action.cost),
          o.action.crewDays,
          fmt(o.expectedEffect),
        ]),
      )}<p>Стоимость ${fmt(p.cost)} млн ₽ включает ${fmt(p.baselineCost)} млн ₽ обязательной базовой остановки. Дополнительные затраты к базе: ${fmt(p.incrementalCost)} млн ₽.</p><p>Формула: Σ pₛ × (ΔQ × kₛ × маржаₛ − дополнительные затраты − риск-резерв × rₛ).</p><p class="muted">${esc(a.warnings[2])}</p>`,
    );
  }
  if (page === "recommendations") {
    content.innerHTML = a.recommendations
      .map((r) =>
        card(
          `${esc(r.wellName)} · ${esc(r.action)} ${r.mandatory ? '<span class="badge bad">Обязательно</span>' : ""}`,
          `<p>${esc(r.explanation)}</p><p>Ожидаемый эффект: <b>${fmt(r.expectedEffect)} млн ₽</b></p>${r.rules.map((x) => `<div class="rule"><b>${esc(x.id)}</b> · ${esc(x.conclusion)}<br><small>${esc(x.facts)}</small></div>`).join("")}`,
        ),
      )
      .join("");
  }
  if (page === "ahp") renderAhp();
  if (page === "scenarios") renderScenarios();
}
async function renderWells() {
  const wells = await api("/wells");
  $("#content").innerHTML =
    `<div class="section-head"><p class="muted">До 12 скважин. Входные признаки и технологические допуски изменяются в карточке.</p><button id="addwell">Добавить скважину</button></div>` +
    card(
      "Реестр фонда",
      table(
        [
          "Скважина / ответственный",
          "q7, т/сут",
          "Вода",
          "Допуск",
          "Данные",
          "Действия",
        ],
        wells.map((w) => [
          `${esc(w.name)}<br><small>${esc(w.owner)}</small>`,
          fmt(w.features.q7),
          fmt(w.features.waterCut * 100, 0) + "%",
          `<span class="badge ${w.productionPermit ? "" : "bad"}">${w.productionPermit ? "Подтвержден" : "Только остановка"}</span>`,
          `<span class="badge ${w.dataValid ? "" : "bad"}">${w.dataValid ? "Проверены" : "Проверить"}</span>`,
          `<div class="actions">${button("Карточка", `data-edit="${esc(w.id)}"`)}${button("Мероприятия", `data-actions="${esc(w.id)}"`)}${button("Удалить", `data-delete="${esc(w.id)}"`)}</div>`,
        ]),
      ),
    );
  $("#addwell").onclick = () => editWell();
  document
    .querySelectorAll("[data-edit]")
    .forEach(
      (b) =>
        (b.onclick = () =>
          editWell(wells.find((w) => w.id === b.dataset.edit))),
    );
  document
    .querySelectorAll("[data-actions]")
    .forEach(
      (b) =>
        (b.onclick = () =>
          editActions(wells.find((w) => w.id === b.dataset.actions)).catch(
            error,
          )),
    );
  document.querySelectorAll("[data-delete]").forEach(
    (b) =>
      (b.onclick = () => {
        openDialog(
          "Удаление скважины",
          `<p>Удалить ${esc(wells.find((w) => w.id === b.dataset.delete).name)} и ее мероприятия? Сохраненные решения останутся в журнале.</p>`,
          async () => {
            await api(
              "/wells/" + encodeURIComponent(b.dataset.delete),
              "DELETE",
            );
            await calculate();
          },
          "Удалить",
        );
      }),
  );
}
function field(name, label, value, type = "number", extra = "") {
  return `<label>${label}<input name="${name}" type="${type}" value="${esc(value)}" ${extra} required></label>`;
}
function editWell(w) {
  const f = w?.features || {
    q7: 30,
    waterCut: 0.35,
    pressure: 8,
    ageMonths: 12,
    trend: -1,
  };
  openDialog(
    w ? "Карточка скважины" : "Новая скважина",
    `<div class="formgrid">${field("name", "Название", w?.name || "", "text", 'maxlength="80"')}${field("owner", "Ответственный", w?.owner || "Цех добычи", "text", 'maxlength="100"')}${field("q7", "Средний дебит за 7 суток, т/сут", f.q7, "number", 'min="0" max="1000" step="any"')}${field("waterCut", "Доля воды, от 0 до 1", f.waterCut, "number", 'min="0" max="1" step="any"')}${field("pressure", "Давление, МПа", f.pressure, "number", 'min="0" max="100" step="any"')}${field("ageMonths", "Возраст, месяцев", f.ageMonths, "number", 'min="0" max="600" step="any"')}${field("trend", "Тренд дебита, т/сут", f.trend, "number", 'min="-1000" max="1000" step="any"')}${field("riskScore", "Технологический риск, 1–5", w?.riskScore || 3, "number", 'min="1" max="5" step="1"')}${field("urgency", "Срочность, 1–5", w?.urgency || 3, "number", 'min="1" max="5" step="1"')}<label><input name="productionPermit" type="checkbox" ${w?.productionPermit !== false ? "checked" : ""}> Допуск подтвержден</label><label><input name="dataValid" type="checkbox" ${w?.dataValid !== false ? "checked" : ""}> Данные проверены</label><label><input name="repairNeeded" type="checkbox" ${w?.repairNeeded ? "checked" : ""}> Требуется рассмотреть ремонт</label></div>`,
    async () => {
      const form = $("#dialogform"),
        data = new FormData(form),
        features = {};
      for (const k of Object.keys(f)) features[k] = Number(data.get(k));
      const r = {
        name: data.get("name"),
        owner: data.get("owner"),
        features,
        riskScore: Number(data.get("riskScore")),
        urgency: Number(data.get("urgency")),
        version: w?.version || 0,
      };
      for (const k of ["productionPermit", "dataValid", "repairNeeded"])
        r[k] = form.elements[k].checked;
      await api(
        "/wells" + (w ? "/" + encodeURIComponent(w.id) : ""),
        w ? "PUT" : "POST",
        r,
      );
      await calculate();
    },
  );
}
async function editActions(w) {
  const actions = await api("/wells/" + encodeURIComponent(w.id) + "/actions");
  openDialog(
    "Мероприятия · " + w.name,
    `<p class="muted">Оценки прироста включают простой и относятся ко всему горизонту 30 суток. Резерв — денежная оценка технологического риска.</p>${table(
      [
        "Действие",
        "Стоимость, млн ₽",
        "Дни",
        "Прирост, т",
        "Резерв, млн ₽",
        "Включено",
      ],
      actions.map((a, i) => [
        esc(a.title),
        `<input aria-label="Стоимость ${esc(a.title)}" name="cost${i}" type="number" step="any" min="0" value="${a.cost}" ${a.kind === "CONTINUE" ? "readonly" : ""}>`,
        `<input aria-label="Дни ${esc(a.title)}" name="days${i}" type="number" min="0" max="60" step="1" value="${a.crewDays}" ${a.kind === "CONTINUE" ? "readonly" : ""}>`,
        `<input aria-label="Прирост ${esc(a.title)}" name="gain${i}" type="number" step="any" value="${a.gainTonnes}" ${["STOP", "CONTINUE"].includes(a.kind) ? "readonly" : ""}>`,
        `<input aria-label="Резерв ${esc(a.title)}" name="risk${i}" type="number" min="0" step="any" value="${a.riskReserve}" ${["STOP", "CONTINUE"].includes(a.kind) ? "readonly" : ""}>`,
        `<input aria-label="Включить ${esc(a.title)}" name="enabled${i}" type="checkbox" ${a.enabled ? "checked" : ""} ${["STOP", "CONTINUE"].includes(a.kind) ? "disabled" : ""}>`,
      ]),
    )}`,
    async () => {
      const form = $("#dialogform");
      const next = actions.map((a, i) => ({
        ...a,
        cost: Number(form.elements["cost" + i].value),
        crewDays: Number(form.elements["days" + i].value),
        gainTonnes: Number(form.elements["gain" + i].value),
        riskReserve: Number(form.elements["risk" + i].value),
        enabled: form.elements["enabled" + i].checked,
      }));
      await api("/wells/" + encodeURIComponent(w.id) + "/actions", "PUT", {
        wellVersion: w.version,
        actions: next,
      });
      await calculate();
    },
  );
}
function renderAhp() {
  const a = analysis.ahp;
  $("#content").innerHTML =
    card(
      "Метод анализа иерархий · AHP",
      `<p>ЛПР сравнивает важность критериев попарно. Значение 2 означает, что критерий строки вдвое важнее критерия столбца. Обратное сравнение рассчитывается автоматически.</p>${table(
        ["Критерий", ...a.criteria.map(esc)],
        a.matrix.map((row, i) => [
          esc(a.criteria[i]),
          ...row.map((v, j) =>
            j > i
              ? `<input aria-label="Сравнение ${i + 1} и ${j + 1}" class="ahpinput" data-i="${i}" data-j="${j}" type="number" min="0.111111111" max="9" step="any" value="${v}">`
              : fmt(v, 3),
          ),
        ]),
      )}<p>λmax = ${fmt(a.lambdaMax, 5)}; CI = ${fmt(a.consistencyIndex, 5)}; CR = ${fmt(a.consistencyRatio, 5)} <span class="badge">Согласовано · CR ≤ 0,10</span></p><button id="applyahp">Применить сравнения</button><p class="muted">Матрица определяет приоритет внимания. Экономический портфель рассчитывается отдельно.</p>`,
    ) +
    `<div class="grid">${card("Веса критериев", bars(a.criteria.map((n, i) => [n, a.weights[i] * 100, "%"])))}${card(
      "Рейтинг скважин",
      table(
        ["Скважина", "Приоритет"],
        analysis.wells
          .filter((w) => w.priority != null)
          .sort((a, b) => b.priority - a.priority)
          .map((w) => [esc(w.well.name), fmt(w.priority * 100) + "%"]),
      ) +
        '<p class="muted">Скважины без допуска исключены из рейтинга и требуют обязательной остановки.</p>',
    )}${card(
      "Матрицы сравнений альтернатив",
      analysis.alternativeMatrices
        .map(
          (m, c) =>
            `<h3>${esc(a.criteria[c])}</h3>${table(
              [
                "",
                ...analysis.wells
                  .filter((w) => w.priority != null)
                  .map((w) => esc(w.well.name)),
              ],
              m.map((r, i) => [
                esc(
                  analysis.wells.filter((w) => w.priority != null)[i].well.name,
                ),
                ...r.map((v) => fmt(v, 3)),
              ]),
            )}`,
        )
        .join("") +
        '<p class="muted">Для альтернатив используется отношение оценок; нулевые оценки заменяются ε = 10⁻⁶. Матрицы согласованы по построению.</p>',
      "wide",
    )}</div>`;
  $("#applyahp").onclick = async () => {
    const matrix = a.matrix.map((r) => [...r]);
    document.querySelectorAll(".ahpinput").forEach((el) => {
      const i = +el.dataset.i,
        j = +el.dataset.j,
        v = +el.value;
      matrix[i][j] = v;
      matrix[j][i] = 1 / v;
    });
    request.matrix = matrix;
    await calculate();
  };
}
function renderScenarios() {
  const u = analysis.uncertainty;
  $("#content").innerHTML =
    card(
      "Сценарные предположения",
      `<p>Вероятности должны суммироваться до 1. Маржа — млн ₽ за дополнительную тонну. Множитель прироста учитывает неопределенность эффекта мероприятий.</p>${table(
        [
          "Сценарий",
          "Вероятность",
          "Множитель прироста",
          "Маржа, млн ₽/т",
          "Множитель риска",
        ],
        request.scenarios.map((s, i) => [
          esc(s.name),
          ...["probability", "gainFactor", "margin", "riskFactor"].map(
            (k) =>
              `<input aria-label="${esc(s.name)} ${k}" data-scene="${i}" data-key="${k}" type="number" min="0" step="any" value="${s[k]}">`,
          ),
        ]),
      )}<button id="applyscenarios">Применить сценарии</button>`,
    ) +
    card(
      "Сравнение допустимых стратегий",
      table(
        [
          "Стратегия",
          ...request.scenarios.map((s) => esc(s.name)),
          "Байес",
          "Вальд",
          "Сэвидж",
          "Гурвиц",
        ],
        u.strategies.map((s) => [
          esc(s.name),
          ...s.outcomes.map((v) => fmt(v, 3)),
          fmt(s.expectedEffect, 3),
          fmt(s.worstEffect, 3),
          fmt(s.maxRegret, 3),
          fmt(s.hurwicz, 3),
        ]),
      ) +
        `<p>Выбор по Байесу: <b>${esc(u.bayes)}</b>; по Вальду: ${esc(u.wald)}; по Сэвиджу: ${esc(u.savage)}; по Гурвицу: ${esc(u.hurwicz)} (α=${u.alpha}).</p><p class="muted">Все значения — млн ₽ относительно базовой стратегии. Сэвидж минимизирует максимальное сожаление. В сравнение включены только допустимые по ресурсам стратегии.</p>`,
    ) +
    card(
      "Дерево решений · выбранный портфель",
      `<b>Выбор портфеля</b>${request.scenarios.map((s, i) => `<div class="branch">${esc(s.name)} · p=${fmt(s.probability)} → <b>${fmt(u.strategies.at(-1).outcomes[i], 3)} млн ₽</b></div>`).join("")}`,
    );
  $("#applyscenarios").onclick = async () => {
    document
      .querySelectorAll("[data-scene]")
      .forEach(
        (el) =>
          (request.scenarios[+el.dataset.scene][el.dataset.key] = +el.value),
      );
    await calculate();
  };
}
async function renderKnowledge() {
  const rules = await api("/knowledge");
  $("#content").innerHTML =
    card(
      "Продукционная база знаний",
      "<p>Сущности: скважина → признаки → допуск → действие → сценарий → портфель → решение. Правила выполняются до и после оптимизации.</p>" +
        rules
          .map(
            (r) =>
              `<div class="rule"><b>${esc(r.id)} · ЕСЛИ</b> ${esc(r.condition)}<br><b>ТО</b> ${esc(r.conclusion)}</div>`,
          )
          .join(""),
    ) +
    (analysis
      ? card(
          "Сработавшие правила",
          analysis.recommendations
            .flatMap((r) =>
              r.rules.map(
                (k) =>
                  `<p><b>${esc(r.wellName)} · ${esc(k.id)}</b> — ${esc(k.conclusion)}<br><small>Факт: ${esc(k.facts)}</small></p>`,
              ),
            )
            .join("") || "<p>Нет сработавших правил.</p>",
        )
      : "");
}
async function renderML() {
  const [m, status, versions] = await Promise.all([
    api("/ml/model"),
    api("/ml/training"),
    api("/ml/versions"),
  ]);
  const r = m.report;
  $("#content").innerHTML =
    `<div class="callout">Модель оценивает <b>средний базовый дебит нефти на следующие 30 суток</b>, т/сут. При первом запуске обучается на демонстрационной истории; далее загружается сохраненная версия. ГТМ и ремонт в целевую переменную не входят.</div><div class="metrics">${metric("Test MAE", fmt(r.metrics.mae, 3), "т/сут · средняя абсолютная ошибка")}${metric("Test RMSE", fmt(r.metrics.rmse, 3), "т/сут · чувствительность к крупным ошибкам")}${metric("Test R²", fmt(r.metrics.r2, 4), "Качество на отложенных периодах")}${metric("Базовая MAE", fmt(r.metrics.baselineMae, 3), "Прогноз «дебит равен q7»")}</div><div class="grid">${card("Обучение и временное разделение", `<p>${esc(r.engine)}, λ=${r.lambda}. Ridge-регрессия; коэффициенты доступны ниже.</p><p>Train: ${r.trainCount} строк, периоды ${r.trainPeriods.join(", ")}.<br>Validation: ${r.validationCount}, периоды ${r.validationPeriods.join(", ")}.<br>Test: ${r.testCount}, периоды ${r.testPeriods.join(", ")}.</p><p>λ выбирается по validation MAE. Test не используется при выборе. Повторные эксперименты на одном тесте требуют нового внешнего контроля.</p><p><b>Источник:</b> ${esc(m.sourceName)}<br><small>${esc(r.provenance)}</small></p><a href="/api/ml/history.csv" download>Скачать историю активной модели · CSV</a>`)}${card("Плановое переобучение", `<p>Расписание: <b>${esc(status.cron)}</b> · ${esc(status.zone)}<br>Следующий запуск: ${esc(status.nextRunAt || "Выключено")}</p><p>Файл: <code>${esc(status.sourcePath)}</code>. Иначе используется загруженный CSV или текущая история. Неизменившиеся данные пропускаются.</p><p>Кандидат активируется, если validation MAE лучше наивной базы и не хуже текущей модели более чем на 2%.</p><label>Новая история CSV<input id="csvfile" type="file" accept=".csv,text/csv"></label><div class="actions"><button id="uploadcsv" class="secondary">Загрузить в очередь</button><button id="train">Обучить сейчас</button></div><p id="trainingresult" class="muted">В очереди: ${esc(status.queuedSource || "Нет новых данных")}</p>`)}${card("Проверить прогноз", `<form id="predictform"><div class="formgrid">${field("q7", "q7, т/сут", 30, "number", 'step="any" min="0"')}${field("waterCut", "Доля воды", 0.35, "number", 'step="any" min="0" max="1"')}${field("pressure", "Давление, МПа", 8, "number", 'step="any" min="0"')}${field("ageMonths", "Возраст, месяцев", 12, "number", 'step="any" min="0"')}${field("trend", "Тренд, т/сут", -1, "number", 'step="any"')}</div><button>Получить прогноз</button></form><div id="prediction"></div>`)}${card(
      "Коэффициенты и версия",
      table(
        ["Признак", "Коэффициент"],
        r.featureNames.map((n, i) => [esc(n), fmt(r.coefficients[i], 5)]),
      ) +
        `<p>Свободный член: ${fmt(r.intercept, 5)}<br><small>Версия: ${esc(m.id)}<br>Обучена: ${esc(m.createdAt)}<br>SHA-256: ${esc(m.fingerprint)}</small></p>`,
    )}</div>` +
    card(
      "Версии моделей",
      table(
        ["Дата", "Источник", "Validation MAE", "Test MAE", "Статус / действие"],
        versions.map((v) => [
          esc(new Date(v.createdAt).toLocaleString("ru-RU")),
          esc(v.sourceName),
          fmt(v.report.validationMetrics.mae, 3),
          fmt(v.report.metrics.mae, 3),
          v.id === m.id
            ? '<span class="badge">Активна</span>'
            : button("Активировать", `data-activate="${esc(v.id)}"`),
        ]),
      ),
    ) +
    card(
      "Журнал обучения",
      table(
        ["Время", "Триггер", "Результат", "Сообщение"],
        status.runs.map((run) => [
          esc(new Date(run.startedAt).toLocaleString("ru-RU")),
          esc(run.trigger),
          esc(run.status),
          esc(run.message),
        ]),
      ),
    );
  $("#predictform").onsubmit = async (e) => {
    e.preventDefault();
    try {
      const features = {};
      new FormData(e.target).forEach((v, k) => (features[k] = Number(v)));
      const p = await api("/ml/predict", "POST", features);
      $("#prediction").innerHTML =
        `<div class="callout" style="margin-top:15px"><b>${fmt(p.dailyOil)} т/сут</b> · ${fmt(p.oil30Days, 0)} т за 30 дней<br>Опорный прогноз: ${fmt(p.referencePrediction)} т/сут</div>${bars(Object.entries(p.contributions).map(([k, v]) => [k, v]))}<p class="muted">Вклады относительно среднего обучающего профиля. ${esc(p.note)}</p>`;
    } catch (err) {
      error(err);
    }
  };
  $("#uploadcsv").onclick = async () => {
    try {
      const f = $("#csvfile").files[0];
      if (!f) throw Error("Выберите CSV");
      if (f.size > 1000000) throw Error("Файл больше 1 МБ");
      const response = await fetch(
        "/api/ml/dataset?name=" + encodeURIComponent(f.name),
        {
          method: "POST",
          headers: { "Content-Type": "text/csv" },
          body: await f.text(),
        },
      );
      const result = await response.json();
      if (!response.ok) throw Error(result.error);
      await renderML();
    } catch (e) {
      error(e);
    }
  };
  $("#train").onclick = async () => {
    const b = $("#train");
    b.disabled = true;
    $("#trainingresult").textContent = "Обучение…";
    try {
      const run = await api("/ml/train", "POST");
      await calculate();
      $("#trainingresult").textContent = run.status + ": " + run.message;
    } catch (e) {
      error(e);
    } finally {
      b.disabled = false;
    }
  };
  document.querySelectorAll("[data-activate]").forEach(
    (b) =>
      (b.onclick = async () => {
        try {
          await api(
            "/ml/versions/" +
              encodeURIComponent(b.dataset.activate) +
              "/activate",
            "POST",
          );
          await calculate();
        } catch (e) {
          error(e);
        }
      }),
  );
}
async function renderDecisions() {
  const ds = await api("/decisions");
  $("#content").innerHTML =
    `<div class="section-head"><p class="muted">Снимок хранит исходные данные, версию модели и результат. Изменения реестра его не меняют.</p><button id="saveplan" ${!analysis ? "disabled" : ""}>Сохранить текущее решение</button></div>` +
    card(
      "История решений",
      ds.length
        ? table(
            ["Название", "Дата", "Бюджет", "Эффект", "Действия"],
            ds.map((d) => [
              esc(d.title),
              esc(new Date(d.createdAt).toLocaleString("ru-RU")),
              fmt(d.cost),
              fmt(d.expectedEffect),
              `<div class="actions">${button("Открыть", `data-open="${esc(d.id)}"`)}${button("JSON", `data-json="${esc(d.id)}"`)}</div>`,
            ]),
          )
        : '<p class="empty">Сохраните первый расчет</p>',
    ) +
    (ds.length >= 2
      ? card(
          "Сравнить решения",
          `<div class="formgrid"><label>Исходное<select id="left">${ds.map((d) => `<option value="${esc(d.id)}">${esc(d.title)}</option>`).join("")}</select></label><label>Сравниваемое<select id="right">${ds.map((d, i) => `<option value="${esc(d.id)}" ${i === 1 ? "selected" : ""}>${esc(d.title)}</option>`).join("")}</select></label></div><button id="compare">Сравнить</button><div id="comparison"></div>`,
        )
      : "");
  $("#saveplan").onclick = () =>
    openDialog(
      "Сохранить решение",
      field(
        "title",
        "Название расчета",
        "План " + new Date().toLocaleDateString("ru-RU"),
        "text",
        'maxlength="100"',
      ),
      async () => {
        await api("/decisions", "POST", {
          title: $("#dialogform").elements.title.value,
          request,
        });
        await renderDecisions();
      },
    );
  document.querySelectorAll("[data-json]").forEach(
    (b) =>
      (b.onclick = async () => {
        try {
          download(
            "fieldpilot-decision.json",
            JSON.stringify(
              await api("/decisions/" + encodeURIComponent(b.dataset.json)),
              null,
              2,
            ),
          );
        } catch (e) {
          error(e);
        }
      }),
  );
  document.querySelectorAll("[data-open]").forEach(
    (b) =>
      (b.onclick = async () => {
        try {
          const d = await api(
            "/decisions/" + encodeURIComponent(b.dataset.open),
          );
          openDialog(
            d.title,
            `<p>Снимок от ${esc(d.createdAt)} · версия ${esc(d.analysis.model.id)}</p>${table(
              ["Скважина", "Действие", "Эффект"],
              d.analysis.plan.actions.map((o) => [
                esc(o.wellName),
                esc(o.action.title),
                fmt(o.expectedEffect),
              ]),
            )}<p>Итого: ${fmt(d.analysis.plan.expectedEffect)} млн ₽</p>`,
            null,
          );
        } catch (e) {
          error(e);
        }
      }),
  );
  if ($("#compare"))
    $("#compare").onclick = async () => {
      try {
        const c = await api(
          "/decisions/compare?left=" +
            encodeURIComponent($("#left").value) +
            "&right=" +
            encodeURIComponent($("#right").value),
        );
        $("#comparison").innerHTML =
          `<p>Изменение стоимости: <b>${fmt(c.costDifference)} млн ₽</b>; эффекта: <b>${fmt(c.effectDifference)} млн ₽</b>.</p><p>Добавлены: ${esc(c.added.join(", ") || "Нет")}<br>Заменены: ${esc(c.removed.join(", ") || "Нет")}</p>`;
      } catch (e) {
        error(e);
      }
    };
}
calculate();
