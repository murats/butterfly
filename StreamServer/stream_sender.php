<?php

$sock = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
$data = "53001";
$len = strlen($data);
socket_sendto($sock, $data, $len, 0, '127.0.0.1', 53000);

socket_bind($sock, '127.0.0.1', $data);

while (($len = socket_recvfrom($sock, $data, 1024, 0, $from, $port)) !== false) {
	echo $data;
}

socket_close($sock);
?>