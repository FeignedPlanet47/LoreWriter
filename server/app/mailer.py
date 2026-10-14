import smtplib
import ssl
from email.message import EmailMessage
from email.utils import formataddr
from html import escape

from .config import settings


def send_verification_email(email: str, display_name: str, code: str) -> None:
    message = EmailMessage()
    message["Subject"] = "Подтверждение email в NovelForge"
    message["From"] = formataddr((settings.smtp_from_name, settings.smtp_from_email))
    message["To"] = email

    greeting = f", {display_name.strip()}" if display_name.strip() else ""
    message.set_content(
        f"Здравствуйте{greeting}!\n\n"
        f"Код подтверждения NovelForge: {code}\n\n"
        f"Код действует {settings.email_verification_code_minutes} минут.\n"
        "Если вы не регистрировались в NovelForge, просто проигнорируйте это письмо.\n"
    )

    safe_name = escape(display_name.strip())
    html_greeting = f", {safe_name}" if safe_name else ""
    message.add_alternative(
        f"""
        <html>
          <body style="font-family:Arial,sans-serif;color:#1c2340;line-height:1.5">
            <h2 style="margin-bottom:8px">NovelForge</h2>
            <p>Здравствуйте{html_greeting}!</p>
            <p>Ваш код подтверждения:</p>
            <div style="font-size:32px;font-weight:700;letter-spacing:8px;margin:18px 0">{code}</div>
            <p>Код действует {settings.email_verification_code_minutes} минут.</p>
            <p style="color:#6b7280;font-size:13px">
              Если вы не регистрировались в NovelForge, просто проигнорируйте это письмо.
            </p>
          </body>
        </html>
        """.strip(),
        subtype="html",
    )

    context = ssl.create_default_context()

    if settings.smtp_use_ssl:
        with smtplib.SMTP_SSL(
            settings.smtp_host,
            settings.smtp_port,
            timeout=settings.smtp_timeout_seconds,
            context=context,
        ) as smtp:
            _authenticate_and_send(smtp, message)
        return

    with smtplib.SMTP(
        settings.smtp_host,
        settings.smtp_port,
        timeout=settings.smtp_timeout_seconds,
    ) as smtp:
        smtp.ehlo()
        if settings.smtp_use_tls:
            smtp.starttls(context=context)
            smtp.ehlo()
        _authenticate_and_send(smtp, message)


def _authenticate_and_send(smtp: smtplib.SMTP, message: EmailMessage) -> None:
    if settings.smtp_username:
        smtp.login(settings.smtp_username, settings.smtp_password)
    smtp.send_message(message)
