<?php
$root='/var/www/html/';
$db=new mysqli('db','opencart',getenv('DB_PASSWORD'),'opencart');
$db->set_charset('utf8');
$db->query("SET SESSION sql_mode=''");
if (!$db->query("SHOW TABLES LIKE 'oc_setting'")->num_rows) {
    $lines=file($root.'install/opencart.sql');
    $sql=implode('',array_filter($lines,fn($line)=>!str_starts_with($line,'--') && !str_starts_with($line,'#')));
    $db->multi_query($sql);
    do { if($r=$db->store_result()) $r->free(); } while($db->more_results() && $db->next_result());
    if($db->errno) throw new Exception($db->error);
}
// A separate administrator; seed user 1 remains untouched.
$user=getenv('UI_ADMIN_USER'); $password=getenv('UI_ADMIN_PASSWORD');
if(!$user || !$password) throw new Exception('Missing dedicated QA administrator credentials');
$name=$db->real_escape_string($user);
if(!$db->query("SELECT user_id FROM oc_user WHERE username='$name'")->num_rows){
 $salt=substr(bin2hex(random_bytes(5)),0,9); $hash=sha1($salt.sha1($salt.sha1($password)));
 $db->query("INSERT INTO oc_user SET user_id=10001,user_group_id=1,username='$name',password='$hash',salt='$salt',firstname='QA',lastname='Runner',email='qa@example.test',status=1,date_added=NOW()");
}
$salt=substr(bin2hex(random_bytes(5)),0,9); $hash=sha1($salt.sha1($salt.sha1($password)));
$db->query("UPDATE oc_user SET salt='$salt',password='$hash' WHERE username='$name' AND email='qa@example.test' AND firstname='QA'");
$db->query("DELETE FROM oc_customer_login WHERE email='$name'");
$url=getenv('STORE_URL') ?: 'http://localhost:8080/';
foreach(['catalog','admin'] as $area){
 $app=$area==='admin'?'admin/':'catalog/';
 $s="<?php\n";
 foreach(['HTTP_SERVER'=>$url.($area==='admin'?'admin/':''),'HTTPS_SERVER'=>$url.($area==='admin'?'admin/':''),'HTTP_CATALOG'=>$url,'HTTPS_CATALOG'=>$url,
 'DIR_APPLICATION'=>$root.$app,'DIR_SYSTEM'=>$root.'system/','DIR_IMAGE'=>$root.'image/','DIR_STORAGE'=>$root.'system/storage/',
 'DIR_LANGUAGE'=>$root.$app.'language/','DIR_TEMPLATE'=>$root.$app.'view/'.($area==='admin'?'template/':'theme/'),'DIR_CONFIG'=>$root.'system/config/',
 'DIR_CACHE'=>$root.'system/storage/cache/','DIR_DOWNLOAD'=>$root.'system/storage/download/','DIR_LOGS'=>$root.'system/storage/logs/',
 'DIR_MODIFICATION'=>$root.'system/storage/modification/','DIR_SESSION'=>$root.'system/storage/session/','DIR_UPLOAD'=>$root.'system/storage/upload/',
 'DIR_CATALOG'=>$root.'catalog/','CACHE_HOSTNAME'=>'redis','CACHE_PORT'=>'6379','CACHE_PREFIX'=>'qa.','DB_DRIVER'=>'mysqli','DB_HOSTNAME'=>'db','DB_USERNAME'=>'opencart','DB_PASSWORD'=>getenv('DB_PASSWORD'),
 'DB_DATABASE'=>'opencart','DB_PORT'=>'3306','DB_PREFIX'=>'oc_'] as $k=>$v) $s.='define('.var_export($k,true).','.var_export($v,true).");\n";
 file_put_contents($root.($area==='admin'?'admin/':'').'config.php',$s);
}
$config=file_get_contents($root.'system/config/default.php');
$config=str_replace("\$_['cache_engine']         = 'file';","\$_['cache_engine']         = 'redis';",$config);
file_put_contents($root.'system/config/default.php',$config);
function setting($db,$code,$key,$value,$serialized=0){
 $stmt=$db->prepare('SELECT setting_id FROM oc_setting WHERE store_id=0 AND `key`=?'); $stmt->bind_param('s',$key);$stmt->execute();
 if($stmt->get_result()->num_rows){$stmt=$db->prepare('UPDATE oc_setting SET value=?,serialized=? WHERE store_id=0 AND `key`=?');$stmt->bind_param('sis',$value,$serialized,$key);}
 else {$stmt=$db->prepare('INSERT INTO oc_setting (store_id,code,`key`,value,serialized) VALUES (0,?,?,?,?)');$stmt->bind_param('sssi',$code,$key,$value,$serialized);}
 $stmt->execute();
}
$api=$db->query("SELECT api_id FROM oc_api WHERE username='QA Fixture API'")->fetch_assoc();
if(!$api){$key=bin2hex(random_bytes(32));$db->query("INSERT INTO oc_api SET username='QA Fixture API',`key`='$key',status=1,date_added=NOW(),date_modified=NOW()");$api=['api_id'=>$db->insert_id];}
setting($db,'config','config_api_id',(string)$api['api_id']);
// Deterministic settings in this dedicated disposable store.
foreach(['config_url'=>$url,'config_ssl'=>$url,'config_currency'=>'USD','config_currency_auto'=>'0','config_tax'=>'0','config_stock_checkout'=>'0','config_stock_display'=>'1','config_product_count'=>'1','config_limit_catalog'=>'3','config_customer_price'=>'0','config_encryption'=>hash('sha256',$password),'config_email'=>'qa@example.test','config_mail_engine'=>'mail'] as $k=>$v) setting($db,'config',$k,$v);
foreach(['payment_cod_status'=>'1','payment_cod_order_status_id'=>'1','payment_cod_sort_order'=>'1','payment_cod_geo_zone_id'=>'0','payment_cod_total'=>'0'] as $k=>$v) setting($db,'payment_cod',$k,$v);
foreach(['shipping_free_status'=>'1','shipping_free_total'=>'0','shipping_free_geo_zone_id'=>'0','shipping_free_sort_order'=>'1'] as $k=>$v) setting($db,'shipping_free',$k,$v);
foreach(['sub_total'=>1,'shipping'=>3,'coupon'=>4,'voucher'=>5,'total'=>9] as $code=>$sort){setting($db,'total_'.$code,'total_'.$code.'_status','1');setting($db,'total_'.$code,'total_'.$code.'_sort_order',(string)$sort);}
foreach(['payment'=>'cod','shipping'=>'free'] as $type=>$code) if(!$db->query("SELECT extension_id FROM oc_extension WHERE type='$type' AND code='$code'")->num_rows) $db->query("INSERT INTO oc_extension SET type='$type',code='$code'");
// Native stock cancellation: config_complete_status and processing status control restocking.
setting($db,'config','config_processing_status','[1,2]',1);setting($db,'config','config_complete_status','[5]',1);
$db->query("UPDATE oc_currency SET value=1 WHERE code='USD'");
// A minimal owned QA language pack, visibly translated labels on three pages.
foreach(['catalog','admin'] as $area){
 $src=$root.$area.'/language/en-gb';$dest=$root.$area.'/language/qa-es';
 if(!is_dir($dest)){mkdir($dest,0777,true);$it=new RecursiveIteratorIterator(new RecursiveDirectoryIterator($src,FilesystemIterator::SKIP_DOTS),RecursiveIteratorIterator::SELF_FIRST);foreach($it as $f){$p=$dest.'/'.substr($f->getPathname(),strlen($src)+1);if($f->isDir())mkdir($p,0777,true);else copy($f->getPathname(),$p);}}
}
foreach(['common/home.php'=>"\$_['heading_title']='Inicio QA';",'checkout/cart.php'=>"\$_['heading_title']='Carrito QA';",'account/login.php'=>"\$_['heading_title']='Acceso QA';",'account/account.php'=>"\$_['text_my_account']='Cuenta QA';",'account/order.php'=>"\$_['heading_title']='Pedidos QA';"] as $file=>$line) file_put_contents($root.'catalog/language/qa-es/'.$file,"\n".$line."\n",FILE_APPEND);
if(!$db->query("SELECT language_id FROM oc_language WHERE code='qa-es'")->num_rows) $db->query("INSERT INTO oc_language SET name='QA Spanish',code='qa-es',locale='es_ES.UTF-8,es_ES,es-es,spanish',status=1,sort_order=2");
// No installer exposed on the running store.
file_put_contents('/etc/apache2/conf-enabled/qa-installer.conf',"<Directory /var/www/html/install>\nRequire all denied\n</Directory>\n");
exec('chown -R www-data:www-data /var/www/html');
echo "QA store ready\n";
