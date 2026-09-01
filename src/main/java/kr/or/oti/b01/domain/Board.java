package kr.or.oti.b01.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.BatchSize;

import kr.or.oti.b01.dto.BoardListAllDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "imageSet")
public class Board extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;

    @Column(length = 500, nullable = false) //컬럼의 길이와 null허용여부
    private String title;

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(length = 50, nullable = false)
    private String writer;

    
    @OneToMany(mappedBy = "board",
    		cascade = {CascadeType.ALL},
    		fetch = FetchType.LAZY,
    		orphanRemoval = true
    		)
    @Builder.Default
    @BatchSize(size = 20)
    private Set<BoardImage> imageSet = new HashSet<>();
    
//    @OneToMany(mappedBy = "board",
//    		cascade = {CascadeType.ALL},
//    		fetch = FetchType.LAZY)
//    @Builder.Default
//    private List<Reply> replyList= new ArrayList<>();
    
    public void change(String title, String content){
        this.title = title;
        this.content = content;
    }
    
    public void addImage(String uuid, String filename) {
    	BoardImage boardImage = BoardImage.builder()
    			.uuid(uuid)
    			.filename(filename)
    			.board(this)
    			.ord(imageSet.size())
    			.build();
    	
    	imageSet.add(boardImage);
    }
    
    public void clearImages() {
//    	for(BoardImage image: imageSet) {
//    		image.changeBoard(null);
//    	}
    	imageSet.forEach(image -> image.changeBoard(null));
    	imageSet.clear();
    }
    
    
    public BoardListAllDTO of(Long replyCount) {
    	return BoardListAllDTO.builder()
    			.bno(bno)
    			.title(title)
    			.writer(writer)
    			.regDate(getRegDate())
    			.replyCount(replyCount)
    			.build();
    }
    
    
}