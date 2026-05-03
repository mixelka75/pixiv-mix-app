package wtf.mxl.pixmix.shared.data

import wtf.mxl.pixmix.shared.data.api.dto.IllustDetailDto
import wtf.mxl.pixmix.shared.data.api.dto.IllustPageDto
import wtf.mxl.pixmix.shared.data.api.dto.IllustThumbnailDto
import wtf.mxl.pixmix.shared.domain.model.AuthorSummary
import wtf.mxl.pixmix.shared.domain.model.IllustDetail
import wtf.mxl.pixmix.shared.domain.model.IllustKind
import wtf.mxl.pixmix.shared.domain.model.IllustPage
import wtf.mxl.pixmix.shared.domain.model.IllustSummary
import wtf.mxl.pixmix.shared.domain.model.XRestrict

fun IllustThumbnailDto.toDomain(): IllustSummary = IllustSummary(
    id = id,
    title = title,
    kind = when (illustType) {
        1 -> IllustKind.Manga
        2 -> IllustKind.Ugoira
        else -> IllustKind.Illust
    },
    xRestrict = when (xRestrict) {
        1 -> XRestrict.R18
        2 -> XRestrict.R18G
        else -> XRestrict.Safe
    },
    width = width,
    height = height,
    pageCount = pageCount,
    thumbnailUrl = url,
    tags = tags,
    author = AuthorSummary(id = userId, name = userName, avatarUrl = profileImageUrl),
    isMasked = isMasked,
)

fun IllustDetailDto.toDomain(): IllustDetail = IllustDetail(
    id = id,
    title = title,
    description = description,
    kind = when (illustType) {
        1 -> IllustKind.Manga
        2 -> IllustKind.Ugoira
        else -> IllustKind.Illust
    },
    xRestrict = when (xRestrict) {
        1 -> XRestrict.R18
        2 -> XRestrict.R18G
        else -> XRestrict.Safe
    },
    width = width,
    height = height,
    pageCount = pageCount,
    createDate = createDate,
    viewCount = viewCount,
    bookmarkCount = bookmarkCount,
    likeCount = likeCount,
    commentCount = commentCount,
    author = AuthorSummary(id = userId, name = userName, avatarUrl = ""),
    tags = tags.tags.map { it.tag },
    previewUrl = urls.regular.ifBlank { urls.small.ifBlank { urls.thumb } },
    regularUrl = urls.regular,
    originalUrl = urls.original,
    isLiked = likeData,
    isBookmarked = isBookmarked || bookmarkData != null,
    bookmarkId = bookmarkData?.id,
)

fun IllustPageDto.toDomain(): IllustPage = IllustPage(
    regularUrl = urls.regular,
    originalUrl = urls.original,
    width = width,
    height = height,
)
