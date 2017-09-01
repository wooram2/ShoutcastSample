1. 개요.
우리 회사에서는 세이캐스트라는 오디오 방송 서비스를 운영하고 있습니다.
세이캐스트는 개인이 오디오 방송을 운영할 수 있도록 지원하는 서비스입니다.
세이캐스트는 ShoutCast라는 기술을 사용하여 개발되었으며, 
이를 재생할 수 있는 Sample App을 소개해 드리겠습니다.

2. ShoutCast 란.
ShoutCast는 Http 프로토콜과 'MP3 Stream'을 사용하여, 
오디오 방송을 서비스하는 기술입니다.
Http의 content-type은 audio이고,
Http의 Body에 MP3 Header와 Frame으로 구성된 'MP3 Stream'이 전송됩니다.
또한 방송이 종료될 때까지 'MP3 Stream'을 지속적으로 전송해야 하기 때문에,
Http의 content-length는 Null이 됩니다.


3. ShoutCast에서의 메타 데이터 구성.
ShoutCast에서는 방송 정보에 대한 메타 데이터를 받아볼 수 있습니다.
이를 위해 먼저, 'Http Header Field'에 Icy-Metadata를 추가하여 1로 설정해야 합니다.
그러면 Http의 Body에서 일정한 간격으로 메타 데이터가 전송됩니다.
또한 'Http Response Header Field'에서 메타 데이터가 송출되는 간격에 대한 정보을 얻어 올 수 있습니다.
이에 대한 Key 값은 icy-metaint입니다.


4. MediaPlayer vs ExoPlayer
Android MediaPlayer는 ShoutCast용 플레이어로 적합하지 않습니다.
앞서 말씀드린 바와 같이, Http의 Body에는 'mp3 stream' 뿐만아니라 메타데이터가 혼재되어 있습니다.
애초부터 MediaPlayer는 ShoutCast를 지원하지 않았기 때문에,
스트리밍 데이터를 그대로 재생하면 잡음이 발생합니다.
그래서 'mp3 stream'에서 메타데이터를 분리하는 로직이 필요합니다.
그리고 이를 구현하기에 적합한 라이브러리로 ExoPlayer가 있습니다.
ExoPlayer는 구글에서 개발한 오픈소스로 커스터마이징이 가능하기 때문입니다.


5. ExoPlayer의 DataSource.
ExoPlayer를 구성하는 여러개의 컴포넌트 중에서 DataSource라는 Class가 있습니다.
DataSource는 플레이어가 재생할 대상이 되는 Object를 정의합니다.
그리고 DataSource를 Implementation한 Class로 DefaultHttpDataSource라는 Class가 있습니다.
DefaultHttpDataSource는 http 프로토콜을 통해 스트리밍되는 음원 데이터를 관리합니다.


6. 'MP3 stream'에서 메타데이터를 분리하는 로직 구현.
DefaultHttpDataSource의 read 메소드에서 스트리밍 데이터를 일차적으로 처리합니다.
DefaultHttpDataSource를 상속받는 클래스인 IcyDataSource Class를 만들고,
read 메소드를 오버라이드하여 로직을 구현했습니다.

	
7. 로직 실행 순서.
	1) http header에서 메타데이터가 송출되는 간격을 얻어옵니다. 
세이케스트의 경우 16384라는 리턴값을 얻을 수 있습니다.
이 값은 16384 byte 간격으로 메타데이터가 송출된다는 의미입니다.

	2) 16384 byte 간격으로 1byte를 가져옵니다. 
그리고 이 값에 16을 곱합니다. 이는 바로 뒤에 연달아 오는 메타데이터의 길이를 의미합니다.

	3) 앞서 구한 메타데이터의 길이 만큼 메타데이터를 가져옵니다.

	4) 그 외에 나머지 데이터는 DefaultHttpDataSource에서 처리하도록 합니다.

    

8. 샘플 코드.
	- https://github.com/wooram2/ShoutcastSample.git


9. 참고자료.
	- http://www.smackfu.com/stuff/programming/shoutcast.html
	- https://github.com/Ood-Tsen/ExoPlayer/commit/8ccc99bc5c6428760efd9f1780dd90be9386339e
	- https://google.github.io/ExoPlayer/guide.html
	- https://news.realm.io/news/360andev-effie-barak-switching-exoplayer-better-video-android/
