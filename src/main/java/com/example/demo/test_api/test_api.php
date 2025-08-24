<?php

$baseUrl = "http://localhost:8080/api";
$logFile = __DIR__ . "/result.log";

file_put_contents($logFile, "==== API TEST RUN " . date("Y-m-d H:i:s") . " ====\n", LOCK_EX);

function logResult($title, $response, $httpCode) {
    global $logFile;
    $line = "\n### $title\nHTTP $httpCode\n$response\n";
    file_put_contents($logFile, $line, FILE_APPEND | LOCK_EX);
}

function request($method, $url, $data = null) {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_HEADER, true);

    if ($data !== null) {
        $json = json_encode($data, JSON_UNESCAPED_UNICODE);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ["Content-Type: application/json", "Content-Length: " . strlen($json)]);
    }

    $response = curl_exec($ch);
    $headerSize = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
    $headers = substr($response, 0, $headerSize);
    $body = substr($response, $headerSize);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    return [$httpCode, $body];
}

[$code, $body] = request("POST", "$baseUrl/clients", [
    "name" => "Supplier A",
    "email" => "suppA@test.io",
    "address" => "Kyiv"
]);
logResult("Create Supplier", $body, $code);
$supplier = json_decode($body, true);

[$code, $body] = request("POST", "$baseUrl/clients", [
    "name" => "Consumer B",
    "email" => "consB@test.io",
    "address" => "Lviv"
]);
logResult("Create Consumer", $body, $code);
$consumer = json_decode($body, true);

[$code, $body] = request("POST", "$baseUrl/orders", [
    "title" => "order-1",
    "supplierId" => $supplier["id"] ?? 1,
    "consumerId" => $consumer["id"] ?? 2,
    "price" => 100
]);
logResult("Create Order", $body, $code);

[$code, $body] = request("POST", "$baseUrl/orders", [
    "title" => "order-1",
    "supplierId" => $supplier["id"] ?? 1,
    "consumerId" => $consumer["id"] ?? 2,
    "price" => 100
]);
logResult("Duplicate Order", $body, $code);

[$code, $body] = request("GET", "$baseUrl/clients/" . ($supplier["id"] ?? 1) . "/profit");
logResult("Profit Supplier", $body, $code);

[$code, $body] = request("GET", "$baseUrl/clients/" . ($consumer["id"] ?? 2) . "/profit");
logResult("Profit Consumer", $body, $code);

[$code, $body] = request("POST", "$baseUrl/orders", [
    "title" => "bad-price",
    "supplierId" => $supplier["id"] ?? 1,
    "consumerId" => $consumer["id"] ?? 2,
    "price" => 0
]);
logResult("Order with bad price", $body, $code);

[$code, $body] = request("PATCH", "$baseUrl/clients/" . ($consumer["id"] ?? 2) . "/status", [
    "active" => false
]);
logResult("Deactivate Consumer", $body, $code);

[$code, $body] = request("POST", "$baseUrl/orders", [
    "title" => "after-deactivate",
    "supplierId" => $supplier["id"] ?? 1,
    "consumerId" => $consumer["id"] ?? 2,
    "price" => 50
]);
logResult("Order after deactivation", $body, $code);

echo "✅ Тесты завершены. Результаты смотри в $logFile\n";