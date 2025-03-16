import nlpcloud

def main(data, language):
    client = nlpcloud.Client("finetuned-llama-3-70b", "9e3da2410d5b5c45be7011df02e05aaed2fab1e5", gpu=True, lang=language)
    data = client.summarization(data,size="small")
    return data["summary_text"]

