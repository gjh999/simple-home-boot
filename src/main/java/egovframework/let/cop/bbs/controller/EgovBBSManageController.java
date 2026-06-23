package egovframework.let.cop.bbs.controller;

import java.util.List;
import java.util.Map;

import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.service.EgovFileMngService;
import egovframework.com.cmm.service.EgovFileMngUtil;
import egovframework.com.cmm.service.FileVO;
import egovframework.let.cop.bbs.domain.model.Board;
import egovframework.let.cop.bbs.dto.request.BbsManageDeleteBoardRequestDTO;
import egovframework.let.cop.bbs.dto.request.BbsManageDetailBoardRequestDTO;
import egovframework.let.cop.bbs.dto.request.BbsSearchRequestDTO;
import egovframework.let.cop.bbs.dto.response.BbsManageDetailResponseDTO;
import egovframework.let.cop.bbs.dto.response.BbsManageListResponseDTO;
import egovframework.let.cop.bbs.service.EgovBBSAttributeManageService;
import egovframework.let.cop.bbs.service.EgovBBSManageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.exception.EgovBizException;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Thymeleaf 기반 게시물 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/bbs/{bbsId}")
public class EgovBBSManageController {

    @Resource(name = "EgovBBSManageService")
    private EgovBBSManageService bbsMngService;

    @org.springframework.beans.factory.annotation.Autowired
    private egovframework.com.cmm.util.MessageUtil messageUtil;


    @Resource(name = "EgovBBSAttributeManageService")
    private EgovBBSAttributeManageService bbsAttrbService;

    @Resource(name = "propertiesService")
    private EgovPropertyService propertyService;

    @Resource(name = "EgovFileMngService")
    private EgovFileMngService fileMngService;

    @Resource(name = "EgovFileMngUtil")
    private EgovFileMngUtil fileUtil;

    /**
     * 게시물 목록 조회
     */
    @GetMapping("/list")
    public String bbsList(@PathVariable String bbsId,
                          @ModelAttribute BbsSearchRequestDTO search,
                          @AuthenticationPrincipal LoginVO user,
                          Model model) throws Exception {
        search.setBbsId(bbsId);
        if (search.getPageIndex() < 1) search.setPageIndex(1);

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(search.getPageIndex());
        paginationInfo.setRecordCountPerPage(propertyService.getInt("Globals.pageUnit"));
        paginationInfo.setPageSize(propertyService.getInt("Globals.pageSize"));

        BbsManageListResponseDTO result = bbsMngService.selectBoardArticles(search, paginationInfo, "");
        paginationInfo.setTotalRecordCount(result.getResultCnt());
        result.setPaginationInfo(paginationInfo);

        model.addAttribute("bbsId", bbsId);
        model.addAttribute("result", result);
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("search", search);
        model.addAttribute("baseUrl", "/bbs/" + bbsId + "/list");
        model.addAttribute("isAuthenticated", user != null);
        return "let/cop/bbs/bbsList";
    }

    /**
     * 게시물 상세 조회
     */
    @GetMapping("/{nttId}/detail")
    public String bbsDetail(@PathVariable String bbsId,
                            @PathVariable Long nttId,
                            @AuthenticationPrincipal LoginVO user,
                            Model model) throws Exception {
        String uniqId = (user != null) ? user.getUniqId() : null;

        BbsManageDetailBoardRequestDTO req = BbsManageDetailBoardRequestDTO.builder()
                .bbsId(bbsId)
                .nttId(nttId)
                .plusCount(true)
                .lastUpdusrId(uniqId)
                .build();

        BbsManageDetailResponseDTO result = bbsMngService.selectBoardArticle(req);

        model.addAttribute("bbsId", bbsId);
        model.addAttribute("nttId", nttId);
        model.addAttribute("result", result);
        model.addAttribute("loginVO", user);
        return "let/cop/bbs/bbsDetail";
    }

    /**
     * 게시물 등록 폼
     */
    @GetMapping("/write")
    public String bbsWriteForm(@PathVariable String bbsId, Model model, HttpServletRequest request) {
        model.addAttribute("bbsId", bbsId);
        // 등록 실패로 되돌아온 경우 입력값(board)이 flash 로 전달된다.
        // flash 는 렌더링 시점에 모델로 병합되므로, 여기서 빈 Board 로 덮어쓰지 않도록
        // 입력 flash 맵을 직접 확인한다.
        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
        Object flashBoard = (inputFlashMap != null) ? inputFlashMap.get("board") : null;
        if (flashBoard != null) {
            model.addAttribute("board", flashBoard);
        } else if (!model.containsAttribute("board")) {
            model.addAttribute("board", new Board());
        }
        return "let/cop/bbs/bbsWrite";
    }

    /**
     * 게시물 등록 처리
     */
    @PostMapping("/write")
    public String bbsWriteProcess(@PathVariable String bbsId,
                                  @ModelAttribute Board board,
                                  MultipartHttpServletRequest multiRequest,
                                  @AuthenticationPrincipal LoginVO user,
                                  RedirectAttributes ra) {
        try {
            board.setBbsId(bbsId);
            if (user != null) {
                board.setNtcrId(user.getId());
                board.setNtcrNm(user.getName());
                board.setFrstRegisterId(user.getUniqId());
                board.setLastUpdusrId(user.getUniqId());
            }
            board.setUseAt("Y");

            // 첨부파일 처리 (신규 등록 — atchFileId 새로 생성)
            String atchFileId = processUploadedFiles(multiRequest, "");
            board.setAtchFileId(atchFileId);

            bbsMngService.insertBoardArticle(board);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.bbs.posted"));
            return "redirect:/bbs/" + bbsId + "/list";
        } catch (EgovBizException e) {
            // 첨부파일 확장자/크기 등 검증 오류 — 구체적 사유를 안내하고 입력값을 보존한다.
            log.warn("게시물 등록 검증 오류: {}", e.getMessage());
            ra.addFlashAttribute("errorMsg", e.getMessage());
            ra.addFlashAttribute("board", board);
            return "redirect:/bbs/" + bbsId + "/write";
        } catch (Exception e) {
            log.error("게시물 등록 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.bbs.post.error"));
            ra.addFlashAttribute("board", board);
            return "redirect:/bbs/" + bbsId + "/write";
        }
    }

    /**
     * 게시물 수정 폼
     */
    @GetMapping("/{nttId}/update")
    public String bbsUpdateForm(@PathVariable String bbsId,
                                @PathVariable Long nttId,
                                @AuthenticationPrincipal LoginVO user,
                                Model model) throws Exception {
        BbsManageDetailBoardRequestDTO req = BbsManageDetailBoardRequestDTO.builder()
                .bbsId(bbsId)
                .nttId(nttId)
                .plusCount(false)
                .lastUpdusrId(user != null ? user.getUniqId() : null)
                .build();
        BbsManageDetailResponseDTO result = bbsMngService.selectBoardArticle(req);

        model.addAttribute("bbsId", bbsId);
        model.addAttribute("nttId", nttId);
        model.addAttribute("result", result);
        return "let/cop/bbs/bbsUpdate";
    }

    /**
     * 게시물 수정 처리
     */
    @PostMapping("/{nttId}/update")
    public String bbsUpdateProcess(@PathVariable String bbsId,
                                   @PathVariable Long nttId,
                                   @ModelAttribute Board board,
                                   MultipartHttpServletRequest multiRequest,
                                   @AuthenticationPrincipal LoginVO user,
                                   RedirectAttributes ra) {
        try {
            board.setBbsId(bbsId);
            board.setNttId(nttId);
            if (user != null) {
                board.setLastUpdusrId(user.getUniqId());
            }

            // 첨부파일 처리 (기존 atchFileId 가 있으면 이어붙이고, 없으면 새로 생성)
            String atchFileId = processUploadedFiles(multiRequest, board.getAtchFileId());
            board.setAtchFileId(atchFileId);

            bbsMngService.updateBoardArticle(board);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.bbs.updated"));
            return "redirect:/bbs/" + bbsId + "/" + nttId + "/detail";
        } catch (EgovBizException e) {
            log.warn("게시물 수정 검증 오류: {}", e.getMessage());
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/bbs/" + bbsId + "/" + nttId + "/update";
        } catch (Exception e) {
            log.error("게시물 수정 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.bbs.update.error"));
            return "redirect:/bbs/" + bbsId + "/" + nttId + "/update";
        }
    }

    /**
     * 게시물 삭제 처리
     */
    @PostMapping("/{nttId}/delete")
    public String bbsDeleteProcess(@PathVariable String bbsId,
                                   @PathVariable Long nttId,
                                   @AuthenticationPrincipal LoginVO user,
                                   RedirectAttributes ra) {
        try {
            BbsManageDeleteBoardRequestDTO req = new BbsManageDeleteBoardRequestDTO();
            req.setBbsId(bbsId);
            req.setNttId(nttId);

            bbsMngService.deleteBoardArticle(req, user);
            ra.addFlashAttribute("successMsg", messageUtil.get("msg.bbs.deleted"));
        } catch (Exception e) {
            log.error("게시물 삭제 오류", e);
            ra.addFlashAttribute("errorMsg", messageUtil.get("msg.bbs.delete.error"));
        }
        return "redirect:/bbs/" + bbsId + "/list";
    }

    /**
     * 멀티파트 요청의 첨부파일을 저장하고 atchFileId 를 반환한다.
     * <p>실제 파일이 하나도 없으면 기존 atchFileId(없으면 빈 문자열)를 그대로 반환한다.
     * 기존 atchFileId 가 있으면 이어붙이고, 없으면 새 atchFileId 를 생성한다.</p>
     *
     * @param multiRequest      멀티파트 요청
     * @param existingAtchFileId 기존 첨부파일 그룹 ID (신규 등록 시 빈 문자열)
     * @return 첨부파일 그룹 ID (첨부 없으면 기존 값)
     */
    private String processUploadedFiles(MultipartHttpServletRequest multiRequest, String existingAtchFileId) throws Exception {
        String atchFileId = (existingAtchFileId == null) ? "" : existingAtchFileId.trim();

        if (multiRequest == null) {
            return atchFileId;
        }

        final Map<String, MultipartFile> files = multiRequest.getFileMap();
        // 실제 선택된 파일이 있는지 확인 (빈 file input 제외)
        boolean hasRealFile = files.values().stream().anyMatch(
                f -> f != null && f.getOriginalFilename() != null
                        && !f.getOriginalFilename().isEmpty() && f.getSize() > 0);
        if (!hasRealFile) {
            return atchFileId;
        }

        if (atchFileId.isEmpty()) {
            // 신규 첨부 — atchFileId 자동 생성 후 등록
            List<FileVO> result = fileUtil.parseFileInf(files, "BBS_", 0, "", "");
            atchFileId = fileMngService.insertFileInfs(result);
        } else {
            // 기존 첨부에 이어붙이기
            FileVO fvo = new FileVO();
            fvo.setAtchFileId(atchFileId);
            int cnt = fileMngService.getMaxFileSN(fvo);
            List<FileVO> result = fileUtil.parseFileInf(files, "BBS_", cnt, atchFileId, "");
            fileMngService.updateFileInfs(result);
        }
        return atchFileId;
    }
}
