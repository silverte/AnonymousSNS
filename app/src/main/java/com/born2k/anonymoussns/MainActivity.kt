package com.born2k.anonymoussns

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.born2k.anonymoussns.databinding.ActivityMainBinding
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Hours
import org.joda.time.Minutes
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
//    // 로그에 TAG로 사용할 문자열
    val TAG = "MainActivity"
//
//    // 파이어베이스의 test 키를 가진 데이터의 참조 객체를 가져온다.
//    val ref = FirebaseDatabase.getInstance().getReference("test")
    private lateinit var binding: ActivityMainBinding
    // 글 목록을 저장하는 변수
    val posts: MutableList<Post> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // actionbar 의 타이틀을 "글목록" 으로 변경
        supportActionBar?.title = "글목록"

        // 하단의 floatingActionButton 이 클릭될때의 리스터를 설정한다.
        binding.floatingActionButton.setOnClickListener{
            // Intent 생성
            var intent = Intent(this@MainActivity, WriteActivity::class.java)
            // Intent 로 WriteActivity 실행
            startActivity(intent)
        }

        // RecyclerView 에 LayoutManager 설정
        val layoutManager = LinearLayoutManager(this@MainActivity)

        // 리사이클러뷰의 아이템을 역순으로 정렬하게함
        layoutManager.reverseLayout = true
        // 리사이클러뷰의 아이템을 쌓는 순서를 끝부터 쌓게 함
        layoutManager.stackFromEnd = true

        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = MyAdapter()

        // Firebase 에서 Post 데이터를 가져온 후 posts 변수에 저장
        FirebaseDatabase.getInstance("https://anonymoussns-48a6c-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("/Posts").orderByChild("writeTime").addChildEventListener(object: ChildEventListener {
                // 글이 추가된 경우
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let { snapshot ->
                        // snapshot 의 데이터를 Post 객체로 가져옴
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            // 새 글이 마지막 부분에 추가된 경우
                            if(previousChildName == null){
                                // 글 목록을 저장하는 변수에 post 객체 추가
                                posts.add(it)
                                // RecyclerView 의 adapter 에 글이 추가된 것을 알림
                                binding.recyclerView.adapter?.notifyItemInserted(posts.size -1)
                            } else {
                                // 글이 중간에 삽입된 경우 pervChildKey 로 한단계 앞의 데이터의 위치를 찾은 뒤 데이터를 추가한다.
                                val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                                posts.add(prevIndex + 1, post)
                                // RecyclerView 의 adapter 에 글이 추가된 것을 알림
                                binding.recyclerView.adapter?.notifyItemInserted(prevIndex + 1)
                            }
                        }
                    }
                }

                // 글이 변경된 경우
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let { snapshot ->
                        // snapshot 의 데이터를 Post 객체로 가져옴
                        val post = snapshot.getValue(Post::class.java)
                        post?.let { post ->
                            // 글이 변경된 경우 글의 앞의 데이터 인데스에 데이터를 변경한다.
                            val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                            posts[prevIndex + 1] = post
                            binding.recyclerView.adapter?.notifyItemChanged(prevIndex + 1)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    snapshot?.let {
                        // snapshot 의 데이터를 Post 객체로 가져옴
                        val post = snapshot.getValue(Post::class.java)
                        post?.let { post ->
                            // 기존에 저장된 인덱스를 찾아서 해당 인덱스의 데이터를 삭제한다.
                            val existIndex = posts.map{it.postId}.indexOf(post.postId)
                            posts.removeAt(existIndex)
                            binding.recyclerView.adapter?.notifyItemRemoved(existIndex)
                        }
                    }
                }

                // 글의 순서가 이동한 경우
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot?.let {
                        // snapshot 의 데이터를 Post 객체로 가져옴
                        val post = snapshot.getValue(Post::class.java)
                        post?.let { post ->
                            // 기존의 인덱스를 구한다.
                            val exsitIndex = posts.map { it.postId }.indexOf(post.postId)
                            // 기존에 데이터를 지운다.
                            posts.removeAt(exsitIndex)
                            binding.recyclerView.adapter?.notifyItemRemoved(exsitIndex)
                            // previousChildName 이 없는 경우 맨마지막으로 이동 된 것
                            if(previousChildName == null){
                                posts.add(post)
                                binding.recyclerView.adapter?.notifyItemChanged(posts.size -1)
                            } else {
                                // previousChildName 다음 글로 추가
                                val prevIndex = posts.map { it.postId }.indexOf(previousChildName)
                                posts.add(prevIndex + 1, post)
                                binding.recyclerView.adapter?.notifyItemChanged(prevIndex + 1)
                            }
                        }
                   }
                }

                // 취소된 경우
                override fun onCancelled(error: DatabaseError) {
                    // 취소가 된경우 에러를 로그로 보여준다
                    error?.toException()?.printStackTrace()
                }
            })

//        // 값의 변경이 있는 경우의 이벤트 리스너를 추가한다.
//        ref.addValueEventListener(object: ValueEventListener{
//            override fun onCancelled(error: DatabaseError) {
//                error.toException().printStackTrace()
//            }
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//                // test 키를 가진 데이터 스냅샷에서 값을 읽고 문자열로 변경한다.
//                val message = snapshot.value.toString()
//                // 읽은 문자열을 로깅
//                Log.d(TAG, message)
//                // Firebase 에서 전달받은 메세지로 제목을 변경한다.
//                supportActionBar?.title = message
//            }
//        })
    }

    // RecyclerView 에서 사용하는 View 홀더 클래스
    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        // 글의 배경 이미지뷰
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        // 글의 내용 텍스트뷰
        val contentText: TextView = itemView.findViewById(R.id.contentsText)
        // 글쓴 시간 텍스트 뷰
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        // 댓글 개수 텍스트뷰
        val commentCountText: TextView = itemView.findViewById(R.id.commentCountTextView)
    }

    // RecyclerView 의 어댑터 클래스
    inner class MyAdapter: RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(LayoutInflater.from(this@MainActivity)
                                           .inflate(R.layout.card_post, parent, false))
        }

        // RecyclerView 에서 몇개의 행을 그릴지 기준이 되는 메소드
        override fun getItemCount(): Int {
            return posts.size
        }

        // 각 행의 포지션에서 그려야 할 ViewHolder UI 에 데이터를 적용하는 메소드
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = posts[position]
            // 배경 이미지 설정
            Picasso.get().load(Uri.parse(post.bgUri)).fit().centerCrop().into(holder.imageView)
            // 카드에 글을 세팅
            holder.contentText.text = post.message
            // 글이 쓰여진 시간
            holder.timeTextView.text = getDiffTimeText(post.writeTime as Long)
            // 댓글 개수는 현재 상태에서는 0으로 일단 세팅
            FirebaseDatabase.getInstance(applicationContext.getString(R.string.URL))
                .getReference("/Comments").orderByKey().equalTo(post.postId)
                .addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d(TAG, post.postId)
                        holder.commentCountText.text = snapshot.child(post.postId).childrenCount.toString()
                        Log.d(TAG, holder.commentCountText.text as String)
                        Log.d(TAG, snapshot.toString())
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }
                })

            holder.itemView.setOnClickListener {
                // 상세화면을 호출할 intent 를 생성한다.
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                // 선택된 카드의 ID 정보를 intent 에 추가한다.
                intent.putExtra("postId", post.postId)
                // intent 로 상세화면을 시작한다.
                startActivity(intent)
            }
        }
    }

    // 글이 쓰여진 시간을 "방금전", "시간전", "yyyy년 MM월 dd일 HH:mm" 포맷으로 반환해주는 메소드
    fun getDiffTimeText(targetTime: Long): String {
        val curDateTime = DateTime()
        val targetDateTime = DateTime().withMillis(targetTime)

        val diffDay = Days.daysBetween(curDateTime, targetDateTime).days
        val diffHours = Hours.hoursBetween(targetDateTime, curDateTime).hours
        val diffMinutes = Minutes.minutesBetween(targetDateTime, curDateTime).minutes
        if (diffDay == 0) {
            if (diffHours == 0 && diffMinutes == 0) {
                return "방금 전"
            }
            return if (diffHours > 0) {
                "" + diffHours + "시간 전"
            } else "" + diffMinutes + "분 전"
        } else {
            val myFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm")
            return myFormat.format(Date(targetTime))
        }
    }
}