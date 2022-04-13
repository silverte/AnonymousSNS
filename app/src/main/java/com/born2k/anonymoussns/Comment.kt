package com.born2k.anonymoussns

class Comment {
    /**
     * 댓글의 ID
     */
    var commentId = ""
    /**
     * 댓글 대상이되는 글의 ID
     */
    var postId = ""
    /**
     * 댓글 작성자의 아이디
     */
    var writerId = ""
    /**
     * 댓글 내용
     */
    var message = ""
    /**
     * 작성시간
     */
    var writeTime:Any = Any()
    /**
     * 배경이미지
     */
    var bgUri = ""
}