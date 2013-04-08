<?php


class PortListener extends Thread {
	
	public $ports = array();
	
	public function run() {
		$socket = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
		socket_bind($socket, '192.168.1.23', 53000);
		
		while (socket_recvfrom($socket, $buf, 5, 0, $from, $port) !== false) {
			array_push($this->ports, $buf);
			echo "socket recv from" . $buf;
		}
	}
}
$portListener = new PortListener();
$portListener->start();


error_reporting(E_ALL | E_STRICT);

$socket = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
socket_bind($socket, '192.168.1.23', 24455);

$from = '';
$port = 0;

$sock = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);

while (($len = socket_recvfrom($socket, $data, 1024, 0, $from, $port)) !== false) {
	
	foreach ($portListener->ports as $key => $port) {
		
		socket_sendto($sock, $data, $len, 0, '127.0.0.1', $port);
		
	}
	
	
	
}

socket_close($sock);

echo "finished";

?>