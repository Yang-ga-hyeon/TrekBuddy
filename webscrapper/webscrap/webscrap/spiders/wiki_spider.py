import re
import scrapy


class WikiSpider(scrapy.Spider):
    name = 'wiki'
    start_urls = ['https://ko.wikipedia.org/wiki/광명동굴']

    def parse(self, response):
        # 텍스트 추출
        # all_text = response.css('p::text, p b::text, p a::text').getall()
        #
        # all_text = [re.sub(r'\([^)]*\)', '', text) for text in all_text]
        # all_text = [re.sub(r'\[[^]]*\]', '', text) for text in all_text]
        #
        # for index, text in enumerate(all_text):
        #     if text.startswith('p'):
        #         all_text[index] += '\n'
        #
        # for text in all_text:
        #     self.log(f'Text: {text}')
        #
        # self.save_to_txt(all_text)
        # latitude와 longitude 클래스 내부의 텍스트 추출
        latitude_text = response.css('span.latitude::text').get()
        longitude_text = response.css('span.longitude::text').get()

        latitude_decimal = self.extract_and_convert_coordinates(latitude_text)
        longitude_decimal = self.extract_and_convert_coordinates(longitude_text)

        # 추출된 텍스트를 파일에 저장
        with open("광명동굴.txt", "w") as file:
            file.write(f"{latitude_decimal}\n{longitude_decimal}")

        self.log("좌표가 성공적으로 추출되었고, coordinates.txt 파일에 저장되었습니다.")

    def extract_and_convert_coordinates(self, text):
        # 정규표현식을 사용하여 각도, 분, 초 추출
        match = re.match(r'(북위|동경) (\d+)° (\d+)′ ([\d.]+)″', text)
        if match:
            direction, degrees, minutes, seconds = match.groups()
            # 북위일 경우 양수, 동경일 경우 음수로 변환
            sign = 1 if direction == '북위' else -1
            # 각도, 분, 초를 소숫점으로 변환
            decimal_value = round(sign * (int(degrees) + int(minutes) / 60 + float(seconds) / 3600), 6)
            return decimal_value
        else:
            return None

    def save_to_txt(self, data):
        # Open or create a text file
        with open('몽촌토성.txt', 'w', encoding='utf-8') as txtfile:
            # Write the data to the text file
            for paragraph in data:
                txtfile.write(paragraph + ' ')
