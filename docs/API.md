# API

Базовый адрес http://localhost:8081. JSON, деньги в млн ₽, нефть в т и т/сут, вода как доля 0–1. Полные примеры контрактов доступны через GET и `model/request`.

| Метод | Адрес | Назначение |
|---|---|---|
| GET/POST | /api/wells | Список / создать скважину |
| GET/PUT/DELETE | /api/wells/{id} | Карточка / изменить с version / удалить |
| GET/PUT | /api/wells/{id}/actions | Все пять действий / сохранить с wellVersion |
| GET | /api/analysis | Анализ с начальными параметрами |
| POST | /api/analysis | Анализ с бюджетом, ресурсом, matrix и scenarios |
| GET | /api/knowledge | Правила R1–R6 |
| GET | /api/ml/model | Активный артефакт и метрики |
| POST | /api/ml/predict | Прогноз по WellFeatures |
| GET | /api/ml/versions | Сохраненные модели |
| POST | /api/ml/versions/{id}/activate | Ручная смена активной версии |
| GET | /api/ml/history.csv | История активной модели |
| GET | /api/ml/training | Настройки cron, очередь, журнал |
| POST | /api/ml/dataset?name=history.csv | Поставить text/csv в очередь |
| POST | /api/ml/train | Обучить сейчас |
| GET/POST | /api/decisions | Журнал / сохранить снимок с title и request |
| GET/DELETE | /api/decisions/{id} | Снимок / удалить |
| GET | /api/decisions/compare?left=ID&right=ID | Различия двух снимков |
| GET | /actuator/health | Проверка состояния |

```sh
curl -s http://localhost:8081/api/analysis
curl -s -X POST http://localhost:8081/api/analysis \
  -H 'Content-Type: application/json' -d '{"budget":3.5,"crewDays":12}'
curl -s -X POST http://localhost:8081/api/ml/predict \
  -H 'Content-Type: application/json' \
  -d '{"q7":30,"waterCut":0.35,"pressure":8,"ageMonths":12,"trend":-1}'
curl -s -X POST http://localhost:8081/api/ml/dataset?name=history.csv \
  -H 'Content-Type: text/csv' --data-binary @training-data/history.csv
```

Ошибки: 400 неверные данные / несогласованный AHP / вероятности, 404 нет объекта, 409 конфликт версии / имени, 422 недопустимый портфель или превышение лимита поиска. Тело: `{"error":"Описание"}`. Ошибка фонового или ручного обучения представлена статусом FAILED в TrainingRun; успешный HTTP-запрос запуска не означает автоматическую активацию.

CR: `CI=(λmax−4)/3`, `CR=CI/0,90`. Принят порог 0,10. Допустимы взаимно обратные сравнения от 1/9 до 9. Пользовательские сценарии: ровно три, конечные неотрицательные вероятности с суммой 1.
