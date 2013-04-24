<?php


$socket = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
socket_bind($socket, '192.168.1.20', 24455);

$from = '';
$port = 0;

$sock = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);

while (socket_recvfrom($socket, $data, 1024, 0, $from, $port) > 0)
{
	//file_put_contents("deneme", $data);	
	echo $data;
	ob_flush();
}

socket_close($sock);

echo "finished";

?>