#!/bin/bash
# ============================================
# GitLab Registry 수동 배포 스크립트
# ============================================

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================
# 설정
# ============================================
GITLAB_REGISTRY="gitlab.barunsoft.net:6005"
GITLAB_IMAGE="${GITLAB_REGISTRY}/jiasoft/hiteen2-server"
DEFAULT_TAG="0.0.1"

# 태그 입력
read -p "배포할 태그를 입력하세요 [$DEFAULT_TAG]: " TAG
TAG=${TAG:-$DEFAULT_TAG}

echo ""
log_info "=========================================="
log_info "🐳 GitLab Registry 이미지 빌드 & 푸시"
log_info "=========================================="
log_info "Repository: $GITLAB_IMAGE"
log_info "Tag: $TAG (Platform: linux/amd64)"
echo ""

# ============================================
# 1. GitLab Registry 로그인
# ============================================
log_info "GitLab Registry 로그인 중..."
docker login $GITLAB_REGISTRY

# ============================================
# 2. Docker 이미지 빌드 & 푸시
# ============================================
log_info "Docker 이미지 빌드 & 푸시 중..."

docker build \
  --platform linux/amd64 \
  -t ${GITLAB_IMAGE}:prod-${TAG} \
  . --push

log_info "✅ 이미지 빌드 & 푸시 완료!"
echo ""

# ============================================
# 3. 배포 명령어 안내
# ============================================
echo "=========================================="
echo -e "${GREEN}✅ 빌드 & 푸시 완료!${NC}"
echo "=========================================="
echo ""
echo "이미지:"
echo "  - ${GITLAB_IMAGE}:prod-${TAG}"
echo ""
echo "=========================================="
echo "K8s 배포 명령어 (마스터 노드에서 실행):"
echo "=========================================="
echo ""
echo "# 개발 서버 (hiteen-chart 사용)"
echo "helm upgrade --install hiteen ./hiteen-chart \\"
echo "  -n hiteen \\"
echo "  --set app.image.tag=prod-${TAG}"
echo ""
echo "# 운영 서버 (hiteen-app-chart 사용)"
echo "helm upgrade --install hiteen-app ./hiteen-app-chart \\"
echo "  -n hiteen \\"
echo "  -f ./hiteen-app-chart/values.yaml \\"
echo "  --set app.image.tag=prod-${TAG}"
echo ""
