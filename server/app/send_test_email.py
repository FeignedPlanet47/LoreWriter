import sys
from email.message import EmailMessage
from email.utils import formataddr

from .config import settings
from .mailer import send_message


def main() -> None:
    if len(sys.argv) != 2:
        print("Usage: python -m app.send_test_email recipient@example.com")
        raise SystemExit(2)

    recipient = sys.argv[1].strip()
    if "@" not in recipient:
        print("Recipient must be an email address")
        raise SystemExit(2)

    message = EmailMessage()
    message["Subject"] = "NovelForge: тест отправки почты"
    message["From"] = formataddr((settings.smtp_from_name, settings.smtp_from_email))
    message["To"] = recipient
    message.set_content(
        "Это тестовое письмо NovelForge.\n\n"
        "Если вы получили его, сервер успешно отправляет письма через реальный SMTP.\n"
    )
    send_message(message)
    print(f"Test email sent to {recipient}")


if __name__ == "__main__":
    main()
