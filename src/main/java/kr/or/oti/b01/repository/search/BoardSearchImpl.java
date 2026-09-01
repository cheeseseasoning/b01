package kr.or.oti.b01.repository.search;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;

import kr.or.oti.b01.domain.Board;
import kr.or.oti.b01.domain.BoardImage;
import kr.or.oti.b01.domain.QBoard;
import kr.or.oti.b01.domain.QReply;
import kr.or.oti.b01.dto.BoardListAllDTO;
import kr.or.oti.b01.dto.BoardListReplyCountDTO;

public class BoardSearchImpl extends QuerydslRepositorySupport implements BoardSearch {
	
    public BoardSearchImpl(){
        super(Board.class);
    }

    @Override
    public Page<Board> search1(Pageable pageable) {

        QBoard board = QBoard.board;

        JPQLQuery<Board> query = from(board);

        BooleanBuilder booleanBuilder = new BooleanBuilder(); // (

        booleanBuilder.or(board.title.contains("9")); // title like ...

        booleanBuilder.or(board.content.contains("9")); // content like ....

        query.where(booleanBuilder);
        query.where(board.bno.gt(0L));


        //paging
        this.getQuerydsl().applyPagination(pageable, query);

        List<Board> list = query.fetch();

        long count = query.fetchCount();


        return new PageImpl<Board>(list, pageable, count);

    }

    @Override
    public Page<BoardListReplyCountDTO> searchWithReplyCount(String[] types, String keyword, Pageable pageable) {

        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        JPQLQuery<Board> query = from(board);
        
        query.leftJoin(reply).on(reply.board.eq(board));
        query.groupBy(board);

        if( (types != null && types.length > 0) && keyword != null ){ //검색 조건과 키워드가 있다면

            BooleanBuilder booleanBuilder = new BooleanBuilder(); // (

            for(String type: types){

                switch (type){
                    case "t":
                        booleanBuilder.or(board.title.contains(keyword));
                        break;
                    case "c":
                        booleanBuilder.or(board.content.contains(keyword));
                        break;
                    case "w":
                        booleanBuilder.or(board.writer.contains(keyword));
                        break;
                }
            }//end for
            query.where(booleanBuilder);
        }//end if

        //bno > 0
        query.where(board.bno.gt(0L));

        JPQLQuery<BoardListReplyCountDTO> dtoQuery = query.select(Projections.bean(BoardListReplyCountDTO.class,
        		board.bno,
        		board.title,
        		board.writer,
        		board.regDate,
        		reply.count().as("replyCount")
        		));
        
        //paging
        this.getQuerydsl().applyPagination(pageable, dtoQuery);

        List<BoardListReplyCountDTO> list = dtoQuery.fetch();

        long count = dtoQuery.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }

    @Override
    public Page<Board> searchAll(String[] types, String keyword, Pageable pageable) {

        QBoard board = QBoard.board;
        JPQLQuery<Board> query = from(board);

        if( (types != null && types.length > 0) && keyword != null ){ //검색 조건과 키워드가 있다면

            BooleanBuilder booleanBuilder = new BooleanBuilder(); // (

            for(String type: types){

                switch (type){
                    case "t":
                        booleanBuilder.or(board.title.contains(keyword));
                        break;
                    case "c":
                        booleanBuilder.or(board.content.contains(keyword));
                        break;
                    case "w":
                        booleanBuilder.or(board.writer.contains(keyword));
                        break;
                }
            }//end for
            query.where(booleanBuilder);
        }//end if

        //bno > 0
        query.where(board.bno.gt(0L));

        //paging
        this.getQuerydsl().applyPagination(pageable, query);

        List<Board> list = query.fetch();

        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);

    }

    @Override
    public Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, Pageable pageable) {

        QBoard board = QBoard.board;
        QReply reply = QReply.reply;
        JPQLQuery<Board> query = from(board);
        
        query.leftJoin(reply).on(reply.board.eq(board));
        query.groupBy(board);

        if( (types != null && types.length > 0) && keyword != null ){ //검색 조건과 키워드가 있다면

            BooleanBuilder booleanBuilder = new BooleanBuilder(); // (

            for(String type: types){

                switch (type){
                    case "t":
                        booleanBuilder.or(board.title.contains(keyword));
                        break;
                    case "c":
                        booleanBuilder.or(board.content.contains(keyword));
                        break;
                    case "w":
                        booleanBuilder.or(board.writer.contains(keyword));
                        break;
                }
            }//end for
            query.where(booleanBuilder);
        }//end if

        //bno > 0
        query.where(board.bno.gt(0L));
        
        //paging
        this.getQuerydsl().applyPagination(pageable, query);

        JPQLQuery<Tuple> tupleJPQLQuery = query.select(board, reply.countDistinct());

        List<Tuple> tupleList = tupleJPQLQuery.fetch();
        
        List<BoardListAllDTO> dtoList = tupleList.stream().map(tuple -> {
        	//QBoard에서 Board 로 변환   
        	Board board1 = (Board) tuple.get(board);

        	//Board객체를 DTO 객체로 변환 
        	BoardListAllDTO dto = board1.of(tuple.get(1, Long.class));
        	
        	//Board객체에 있는 첨부파일을 객체를 첨부파일 DTO로 객체로 변환 함  
        	dto.setBoardImages(board1.getImageSet().stream().sorted()
					.map(BoardImage::of)
					.collect(Collectors.toList()));
        	
        	return dto;
        	
        }).collect(Collectors.toList());

        long count = tupleJPQLQuery.fetchCount();

        return new PageImpl<>(dtoList, pageable, count);

    }

}