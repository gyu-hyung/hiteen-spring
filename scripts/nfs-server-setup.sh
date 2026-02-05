#!/bin/bash
# ============================================
# NFS 서버 설정 스크립트
# Rocky Linux / AlmaLinux
# ============================================

set -e

echo "=========================================="
echo "💾 NFS 서버 설정 스크립트"
echo "=========================================="

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# Root 권한 확인
if [ "$EUID" -ne 0 ]; then
    echo "이 스크립트는 root 권한으로 실행해야 합니다."
    exit 1
fi

# 네트워크 대역 입력
read -p "허용할 네트워크 대역을 입력하세요 (예: 10.8.0.0/24): " NETWORK_CIDR

if [ -z "$NETWORK_CIDR" ]; then
    NETWORK_CIDR="10.8.0.0/24"
    log_warn "기본값 사용: ${NETWORK_CIDR}"
fi

# ==========================================
# 1. NFS 패키지 설치
# ==========================================
log_info "NFS 패키지 설치 중..."
dnf install -y nfs-utils

# ==========================================
# 2. 공유 디렉토리 생성
# ==========================================
log_info "공유 디렉토리 생성 중..."
mkdir -p /srv/nfs/assets
mkdir -p /srv/nfs/backup
chmod 777 /srv/nfs/assets
chmod 777 /srv/nfs/backup

# ==========================================
# 3. exports 설정
# ==========================================
log_info "exports 설정 중..."
cat >> /etc/exports << EOF
# Kubernetes NFS Shares
/srv/nfs/assets    ${NETWORK_CIDR}(rw,sync,no_subtree_check,no_root_squash)
/srv/nfs/backup    ${NETWORK_CIDR}(rw,sync,no_subtree_check,no_root_squash)
EOF

# ==========================================
# 4. NFS 서비스 시작
# ==========================================
log_info "NFS 서비스 시작 중..."
systemctl enable --now nfs-server rpcbind
exportfs -rav

# ==========================================
# 5. 방화벽 설정
# ==========================================
log_info "방화벽 설정 중..."
firewall-cmd --permanent --add-service=nfs || true
firewall-cmd --permanent --add-service=rpc-bind || true
firewall-cmd --permanent --add-service=mountd || true
firewall-cmd --reload || true

# ==========================================
# 완료
# ==========================================
echo ""
echo "=========================================="
echo -e "${GREEN}✅ NFS 서버 설정 완료!${NC}"
echo "=========================================="
echo ""
echo "NFS 공유 목록:"
exportfs -v
echo ""
echo "클라이언트(워커 노드)에서 테스트:"
echo "  dnf install -y nfs-utils"
echo "  mount -t nfs $(hostname -I | awk '{print $1}'):/srv/nfs/assets /mnt"
echo "  ls /mnt"
echo "  umount /mnt"
echo ""

