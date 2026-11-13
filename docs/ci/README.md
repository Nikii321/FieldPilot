# Шаблон GitHub Actions

`fieldpilot-ci.yml` выполняет JVM-тесты, собирает Docker Compose и проверяет API. Локально те же проверки выполнены при сборке Dockerfile и запуске `scripts/smoke.py`.

Для включения GitHub Actions перенесите файл в `.github/workflows/ci.yml` и отправьте коммит с авторизацией, имеющей право `workflow`. Текущий токен GitHub CLI не имеет этого права, поэтому шаблон хранится здесь и автоматически не запускается. Базовые команды проверки:

```sh
./gradlew --no-daemon test bootJar
docker compose up --build -d --wait
python3 scripts/smoke.py
```
