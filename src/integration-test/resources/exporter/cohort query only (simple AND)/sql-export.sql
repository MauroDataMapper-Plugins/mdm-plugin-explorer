SELECT
    [people].[patients].[id],
    [people].[patients].[forename],
    [people].[patients].[surname],
    [people].[patients].[dateOfBirth],
    [people].[patients].[age],
    [people].[patients].[height],
    [people].[patients].[exercisesRegularly]
FROM
    [people].[patients]
WHERE
    [people].[patients].[age] >= 18
    AND [people].[patients].[age] <= 65
GO

SELECT
    [medical].[episodes].[id],
    [medical].[episodes].[patientId],
    [medical].[episodes].[description],
    [medical].[episodes].[score],
    [medical].[episodes].[occurredAt],
    [medical].[episodes].[do_not_include]
FROM
    [medical].[episodes]
GO

SELECT
    [medical].[treatments].[id],
    [medical].[treatments].[patientId],
    [medical].[treatments].[description],
    [medical].[treatments].[givenOn],
    [medical].[treatments].[do_not_include]
FROM
    [medical].[treatments]
GO