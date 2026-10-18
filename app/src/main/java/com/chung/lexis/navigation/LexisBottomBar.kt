package com.chung.lexis.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun LexisBottomBar(
    navController: NavHostController,
    reviewDueCount: Int,
) {
    val navBackStackEntry by
    navController.currentBackStackEntryAsState()

    val currentDestination =
        navBackStackEntry?.destination

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainer
            ),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {

            topLevelDestinations.forEach { destination ->

                val selected =
                    currentDestination
                        ?.hierarchy
                        ?.any {
                            it.route == destination.route
                        } == true

                LexisBottomBarItem(
                    destination = destination,
                    selected = selected,
                    badgeCount = if (
                        destination == LexisDestination.Review
                    ) {
                        reviewDueCount
                    } else {
                        0
                    },
                    modifier = Modifier.weight(1f),
                    onClick = {

                        navController.navigate(
                            destination.route
                        ) {

                            popUpTo(
                                navController.graph
                                    .findStartDestination()
                                    .id
                            ) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }

        /*
         * The prototype shows the Android gesture area
         * separately beneath the 80dp app navigation bar.
         *
         * We do not draw a fake gesture handle.
         * Android owns that area.
         */
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(
                    WindowInsets.navigationBars
                ),
        )
    }
}

@Composable
private fun LexisBottomBarItem(
    destination: LexisDestination,
    selected: Boolean,
    badgeCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme
                            .secondaryContainer
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {

            Icon(
                painter = painterResource(
                    id = if (selected) {
                        destination.selectedIconRes
                    } else {
                        destination.unselectedIconRes
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme
                        .onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
                },
            )

            if (badgeCount > 0) {

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 36.dp)
                        .defaultMinSize(
                            minWidth = 18.dp,
                        )
                        .height(18.dp)
                        .clip(
                            RoundedCornerShape(9.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.primary
                        )
                        .padding(
                            horizontal = 5.dp
                        ),
                    contentAlignment = Alignment.Center,
                ) {

                    Text(
                        text = badgeCount.toString(),
                        color =
                            MaterialTheme.colorScheme.onPrimary,
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = destination.label,
            style =
                MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}