package kr.or.oti.b01.domain;
import lombok.*;
import javax.persistence.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "board")
@Table(name = "Reply", indexes = {
	@Index(name = "idx_reply_board_bno", columnList = "board_bno")
})
public class Reply extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;

    @Column(length = 2000, nullable = false)
    private String replyText;

    @Column(length = 50, nullable = false)
    private String replyer;

    @ManyToOne(fetch = FetchType.LAZY)
    private Board board;

    // ============ 신규 추가: 댓글 내용만 변경하는 메서드 ============
    public void changeText(String replyText) {
        this.replyText = replyText;
    }
}