package com.example.myapp.helpers;

import com.example.myapp.data.ChatMessage;

import java.util.*;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class SmartChatBotHelper {
    private static final Map<String, List<BotResponse>> responsePatterns = new HashMap<>();

    static class BotResponse {
        String[] patterns;
        String[] responses;
        boolean requiresExactMatch;

        BotResponse(String[] patterns, String[] responses, boolean requiresExactMatch) {
            this.patterns = patterns;
            this.responses = responses;
            this.requiresExactMatch = requiresExactMatch;
        }
    }

    static {
        // Khởi tạo các mẫu câu và câu trả lời
        initializeResponses();
    }

    private static void initializeResponses() {
        // Chào hỏi
        addResponse("greeting", new BotResponse(
                new String[]{"chao", "hello", "hi", "xin chao", "alo"},
                new String[]{
                        """
Xin chào! Tôi là trợ lý ảo, tôi có thể giúp bạn với các thông tin về:
1. Thông tin sản phẩm và giá cả
2. Hướng dẫn đặt hàng
3. Chính sách bảo hành
4. Khuyến mãi hiện có
Bạn cần hỗ trợ vấn đề gì?""",

                        """
Chào bạn! Rất vui được gặp bạn. Tôi có thể giúp bạn những vấn đề sau:
- Tư vấn sản phẩm
- Thông tin giá cả
- Hỗ trợ đặt hàng
- Giải đáp thắc mắc
Bạn cần tôi hỗ trợ gì ạ?"""
                },
                false
        ));

        // Thông tin sản phẩm
        addResponse("product_info", new BotResponse(
                new String[]{"san pham", "thong tin", "chi tiet", "dac diem", "tinh nang"},
                new String[]{
                        """
Để tư vấn chính xác, xin bạn cho biết:
1. Loại sản phẩm bạn quan tâm?
2. Khoảng giá mong muốn?
3. Mục đích sử dụng?
Tôi sẽ gợi ý sản phẩm phù hợp nhất với nhu cầu của bạn."""
                },
                false
        ));

        // Giá cả
        addResponse("price", new BotResponse(
                new String[]{"gia", "gia ca", "bao nhieu", "gia tien"},
                new String[]{
                        """
Để báo giá chính xác, bạn vui lòng cho biết:
1. Tên hoặc mã sản phẩm cụ thể
2. Số lượng cần mua
3. Hình thức thanh toán
Tôi sẽ kiểm tra và báo giá tốt nhất cho bạn, bao gồm cả thông tin về khuyến mãi nếu có."""
                },
                false
        ));

        // Đặt hàng
        addResponse("order", new BotResponse(
                new String[]{"dat hang", "mua", "order", "thanh toan"},
                new String[]{
                        """
Để đặt hàng, bạn cần thực hiện các bước sau:
1. Chọn sản phẩm và số lượng
2. Thêm vào giỏ hàng
3. Điền thông tin giao hàng
4. Chọn phương thức thanh toán
5. Xác nhận đơn hàng

Bạn cần tôi hướng dẫn chi tiết bước nào không?"""
                },
                false
        ));

        // Bảo hành
        addResponse("warranty", new BotResponse(
                new String[]{"bao hanh", "sua chua", "loi", "hong"},
                new String[]{
                        """
Chính sách bảo hành của chúng tôi:
1. Thời gian bảo hành: 12 tháng
2. Phạm vi bảo hành: Lỗi nhà sản xuất
3. Địa điểm: Tất cả trung tâm bảo hành trên toàn quốc
4. Hỗ trợ: 1800.xxxx (miễn phí)

Bạn cần hỗ trợ thêm thông tin gì về bảo hành không?"""
                },
                false
        ));

        // Khuyến mãi
        addResponse("promotion", new BotResponse(
                new String[]{"khuyen mai", "giam gia", "uu dai", "sale"},
                new String[]{
                        """
Hiện tại chúng tôi đang có các chương trình khuyến mãi:
1. Giảm 10% cho đơn hàng trên 1 triệu
2. Tặng phiếu mua hàng 100k cho khách hàng mới
3. Freeship cho đơn từ 500k
4. Giảm thêm 5% khi thanh toán online

Bạn quan tâm đến chương trình nào? Tôi sẽ tư vấn chi tiết hơn."""
                },
                false
        ));

        // Vấn đề kỹ thuật
        addResponse("technical", new BotResponse(
                new String[]{"loi ky thuat", "error", "khong hoat dong", "hu hong"},
                new String[]{
                        """
Để hỗ trợ vấn đề kỹ thuật, vui lòng cung cấp:
1. Mã sản phẩm
2. Mô tả lỗi gặp phải
3. Thời điểm xảy ra lỗi

Hoặc bạn có thể liên hệ trực tiếp hotline: 1800.xxxx (24/7)"""
                },
                false
        ));
    }

    private static void addResponse(String category, BotResponse response) {
        responsePatterns.put(category, Collections.singletonList(response));
    }

    public static String getResponse(String message) {
        message = normalizeText(message.toLowerCase());

        // Tìm câu trả lời phù hợp nhất
        Map<String, Integer> matchScores = new HashMap<>();

        for (Map.Entry<String, List<BotResponse>> entry : responsePatterns.entrySet()) {
            for (BotResponse response : entry.getValue()) {
                int score = calculateMatchScore(message, response.patterns);
                if (score > 0) {
                    matchScores.put(entry.getKey(), score);
                }
            }
        }

        if (!matchScores.isEmpty()) {
            // Lấy category có điểm cao nhất
            String bestMatch = Collections.max(matchScores.entrySet(),
                    Map.Entry.comparingByValue()).getKey();

            List<BotResponse> responses = responsePatterns.get(bestMatch);
            assert responses != null;
            if (!responses.isEmpty()) {
                String[] possibleResponses = responses.get(0).responses;
                return possibleResponses[new Random().nextInt(possibleResponses.length)];
            }
        }

        // Câu trả lời mặc định nếu không tìm thấy kết quả phù hợp
        return """
                Xin lỗi, tôi chưa hiểu rõ câu hỏi của bạn. Bạn có thể:
                1. Diễn đạt lại câu hỏi
                2. Chọn chủ đề cần hỗ trợ: sản phẩm, giá cả, đặt hàng, bảo hành
                3. Liên hệ hotline: 1800.xxxx để được hỗ trợ trực tiếp""";
    }

    private static String normalizeText(String text) {
        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("");
    }

    private static int calculateMatchScore(String message, String[] patterns) {
        int score = 0;
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                score += 1;
            }
        }
        return score;
    }

    public static String analyzeContext(List<ChatMessage> previousMessages, String currentMessage) {
        // Phân tích ngữ cảnh dựa trên các tin nhắn trước
        if (previousMessages != null && !previousMessages.isEmpty()) {
            // Lấy 3 tin nhắn gần nhất để phân tích ngữ cảnh
            int startIndex = Math.max(0, previousMessages.size() - 3);
            List<ChatMessage> recentMessages = previousMessages.subList(startIndex, previousMessages.size());

            // TODO: Thêm logic phân tích ngữ cảnh ở đây
            // Ví dụ: nếu người dùng đang hỏi về giá sau khi đã hỏi về sản phẩm
            // thì có thể trả về giá của sản phẩm đó
        }

        return getResponse(currentMessage);
    }
}