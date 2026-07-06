# PriceCheckLambda — развёртывание в AWS

Собрать fat-jar (из корня основного проекта):
```bash
./mvnw -f lambda/pom.xml clean package
# результат: lambda/target/travel-price-lambda.jar
```

## 1. SNS Topic + подписка на email
```bash
aws sns create-topic --name travel-price-alerts
# запомни ARN, напр.: arn:aws:sns:eu-west-1:123456789012:travel-price-alerts

aws sns subscribe \
  --topic-arn arn:aws:sns:eu-west-1:123456789012:travel-price-alerts \
  --protocol email \
  --notification-endpoint you@example.com
# затем подтверди подписку письмом
```

## 2. IAM-роль исполнения Lambda
Роль должна разрешать запись логов в CloudWatch и публикацию в SNS:
`AWSLambdaBasicExecutionRole` + политика `sns:Publish` на нужный topic ARN.

## 3. Создание функции
```bash
aws lambda create-function \
  --function-name travel-price-checker \
  --runtime java21 \
  --handler com.travelbudget.lambda.PriceCheckLambda::handleRequest \
  --memory-size 512 \
  --timeout 60 \
  --role arn:aws:iam::123456789012:role/travel-lambda-role \
  --zip-file fileb://lambda/target/travel-price-lambda.jar \
  --environment "Variables={API_BASE_URL=https://your-api.com,API_KEY=xxx,SNS_TOPIC_ARN=arn:aws:sns:eu-west-1:123456789012:travel-price-alerts}"
```

Обновление кода после пересборки:
```bash
aws lambda update-function-code \
  --function-name travel-price-checker \
  --zip-file fileb://lambda/target/travel-price-lambda.jar
```

## 4. Запуск по расписанию (EventBridge, каждый час)
```bash
aws events put-rule --name price-check-hourly --schedule-expression "rate(1 hour)"

# разрешить EventBridge вызывать Lambda
aws lambda add-permission \
  --function-name travel-price-checker \
  --statement-id eventbridge-invoke \
  --action lambda:InvokeFunction \
  --principal events.amazonaws.com \
  --source-arn arn:aws:events:eu-west-1:123456789012:rule/price-check-hourly

# привязать функцию как цель правила
aws events put-targets --rule price-check-hourly \
  --targets "Id=1,Arn=arn:aws:lambda:eu-west-1:123456789012:function:travel-price-checker"
```

## 5. CloudWatch
Логи функции пишутся автоматически в `/aws/lambda/travel-price-checker`.
Можно добавить алерт на метрику `Errors > 0`.

---
**Что нужно доделать для прода:** метод `fetchCurrentPrice()` сейчас заглушка —
подставь реальный API цен (Amadeus/Skyscanner). И эндпоинт `GET /api/alerts/active`
на стороне основного API (с проверкой `X-Api-Key`).
