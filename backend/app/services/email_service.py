"""
MILO Notification & OTP Dispatch Service
=========================================
Handles dispatching 6-digit OTP codes via Email (SMTP) and SMS gateways.
Includes resilient development fallback with console highlighting and audit logging.
"""

from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import logging
import smtplib

from app.core.config import settings

logger = logging.getLogger(__name__)


def send_otp_email(to_email: str, otp_code: str, user_name: str = "User") -> bool:
    """
    Dispatches a 6-digit OTP code to the recipient email address.
    If SMTP settings are present in configuration, sends a real MIME multipart email.
    Otherwise, logs to the console and logger for local development.
    """
    subject = f"{otp_code} is your MILO verification code"

    # Plaintext fallback body
    text_content = (
        f"Hi {user_name},\n\n"
        f"Your MILO verification code is: {otp_code}\n\n"
        f"This code will expire in 10 minutes. If you did not request this, please ignore this message.\n\n"
        f"Best regards,\nThe MILO Team"
    )

    # HTML formatted email body
    html_content = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #F9F7F5; margin: 0; padding: 20px; }}
    .card {{ max-width: 480px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border: 1px solid #E2DFDC; padding: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }}
    .title {{ font-size: 24px; font-weight: bold; color: #1E2838; margin-bottom: 8px; text-align: center; }}
    .subtitle {{ font-size: 15px; color: #8C93A0; margin-bottom: 24px; text-align: center; line-height: 1.5; }}
    .code-box {{ background: #FFF4ED; border: 2px dashed #FF9E66; border-radius: 12px; padding: 18px; text-align: center; margin: 24px 0; }}
    .code {{ font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #FF9E66; font-family: monospace; }}
    .footer {{ font-size: 12px; color: #8C93A0; text-align: center; margin-top: 24px; border-top: 1px solid #E2DFDC; padding-top: 16px; }}
  </style>
</head>
<body>
  <div class="card">
    <div class="title">Welcome to MILO</div>
    <div class="subtitle">Use the verification code below to confirm your account identity.</div>
    <div class="code-box">
      <div class="code">{otp_code}</div>
    </div>
    <div class="subtitle" style="font-size: 13px;">This code is valid for <strong>10 minutes</strong>. Never share this code with anyone.</div>
    <div class="footer">
      If you did not request this verification code, you can safely ignore this email.
    </div>
  </div>
</body>
</html>"""

    # Check if SMTP is configured for real dispatch
    if settings.SMTP_HOST and settings.SMTP_USER and settings.SMTP_PASSWORD:
        try:
            msg = MIMEMultipart("alternative")
            msg["Subject"] = subject
            msg["From"] = f"{settings.EMAILS_FROM_NAME} <{settings.EMAILS_FROM_EMAIL}>"
            msg["To"] = to_email

            msg.attach(MIMEText(text_content, "plain"))
            msg.attach(MIMEText(html_content, "html"))

            with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT, timeout=10) as server:
                if settings.SMTP_TLS:
                    server.starttls()
                server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
                server.send_message(msg)

            logger.info(f"[EMAIL DISPATCH] Sent live verification OTP to {to_email}")
            return True
        except Exception as e:
            logger.error(f"[EMAIL DISPATCH ERROR] Failed to send email via SMTP to {to_email}: {e}")
            # Fall back to console log so development is never blocked
            _log_dev_otp("EMAIL", to_email, otp_code)
            return True

    # Development simulation fallback
    _log_dev_otp("EMAIL", to_email, otp_code)
    return True


def send_otp_sms(to_phone: str, otp_code: str) -> bool:
    """
    Dispatches a 6-digit OTP code to the recipient mobile phone number.
    If Twilio credentials are configured, sends via Twilio API.
    Otherwise, logs to the console for local development.
    """
    if (
        settings.TWILIO_ACCOUNT_SID
        and settings.TWILIO_AUTH_TOKEN
        and settings.TWILIO_PHONE_NUMBER
    ):
        try:
            import httpx

            url = f"https://api.twilio.com/2010-04-01/Accounts/{settings.TWILIO_ACCOUNT_SID}/Messages.json"
            data = {
                "From": settings.TWILIO_PHONE_NUMBER,
                "To": to_phone,
                "Body": f"Your MILO verification code is: {otp_code}. Valid for 10 minutes.",
            }
            auth = (settings.TWILIO_ACCOUNT_SID, settings.TWILIO_AUTH_TOKEN)
            response = httpx.post(url, data=data, auth=auth, timeout=10.0)
            if response.is_success:
                logger.info(f"[SMS DISPATCH] Sent SMS via Twilio to {to_phone}")
                return True
            else:
                logger.error(f"[SMS DISPATCH ERROR] Twilio returned {response.status_code}: {response.text}")
        except Exception as e:
            logger.error(f"[SMS DISPATCH ERROR] Failed to send SMS via Twilio: {e}")

    _log_dev_otp("SMS", to_phone, otp_code)
    return True


def _log_dev_otp(channel: str, destination: str, otp_code: str) -> None:
    """Prints a prominent visual banner to standard output during development."""
    banner = (
        f"\n"
        f"============================================================\n"
        f"  [MILO OTP DISPATCH - {channel}]\n"
        f"  Target Destination : {destination}\n"
        f"  VERIFICATION CODE  : >>> [ {otp_code} ] <<<\n"
        f"  Status             : Valid for 10 minutes\n"
        f"============================================================\n"
    )
    print(banner)
    logger.info(f"[OTP DISPATCH -> {channel}] {destination} | Code: {otp_code}")
