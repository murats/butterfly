<?php
//phpinfo();
ob_implicit_flush();

$fp = stream_socket_client("tcp://127.0.0.1:24007", $errno, $errstr, 30);
if (!$fp) {
	echo "$errstr ($errno)<br />\n";
} else {
	//fwrite($fp, "GET / HTTP/1.0\r\nHost: www.example.com\r\nAccept: */*\r\n\r\n");
	while (!feof($fp)) {
		echo fgets($fp, 24);
		ob_flush();
	}
	fclose($fp);
}

?>