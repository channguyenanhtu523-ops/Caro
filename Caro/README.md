# Caro Online Multiplayer

Du an nay la mot tro choi Caro online viet bang Java Core, TCP Socket va Swing. Server quan ly toan bo trang thai van dau de chan gian lan, client hien thi giao dien ban co, chat va trang thai luot choi theo thoi gian thuc.

## Tinh nang chinh

- Kien truc tach lop `common`, `server`, `client`
- TCP socket da luong voi ghep cap 2 nguoi choi
- Handshake RSA, sau do ma hoa AES va kiem tra toan ven bang HMAC-SHA256
- Server xac thuc nuoc di va ghi lich su tran dau ra XML
- Client Swing ve ban co bang custom painting, co hover, highlight nuoc cuoi va duong thang
- XML config cho host, port, kich thuoc ban co va file lich su

## Cau truc thu muc

- `src/com/dmx/caro/common`: protocol, crypto, model, XML utility
- `src/com/dmx/caro/server`: server, client handler, room, history repository
- `src/com/dmx/caro/client`: launcher, controller, network, model, view
- `config/app-config.xml`: cau hinh server va game
- `history/game-history.xml`: duoc tao sau khi co tran dau ket thuc

## Yeu cau

- Java 17 tro len. Moi truong hien tai da co Java 25 LTS.

## Cach chay

1. Chay server:

```bat
run-server.bat
```

2. Mo 2 cua so client:

```bat
run-client.bat
```

3. Nhap `host`, `port`, `username` khac nhau roi bam `Connect`.
4. Khi du 2 nguoi, server tu dong ghep cap va bat dau tran.

## Giao thuc tong quan

- `HELLO` -> client mo ket noi
- `SERVER_HELLO` -> server gui public key RSA va cau hinh
- `KEY_EXCHANGE` -> client gui AES/HMAC da ma hoa bang RSA
- `KEY_ACK` -> server xac nhan kenh bao mat
- `LOGIN` / `LOGIN_RESULT`
- `MATCH_FOUND`
- `MOVE`
- `CHAT`
- `GAME_STATE`
- `GAME_OVER`
- `HEARTBEAT`

Sau handshake, tat ca packet game deu di qua `SecureChannel`.

## Luu y

- Username hop le theo regex `[A-Za-z0-9_]{3,16}`
- Server se tu dong luu lich su vao XML moi khi van dau ket thuc hoac mot nguoi roi phong
- GUI duoc cap nhat tren EDT thong qua `SwingUtilities.invokeLater`
